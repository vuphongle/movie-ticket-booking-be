package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.constant.ValidationMessages;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.entity.CouponDetailTerms;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.TargetType;
import vn.edu.iuh.fit.model.request.UpsertCouponDetailRequest;
import vn.edu.iuh.fit.model.response.CouponDetailResponse;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponDetailTermsRepository;
import vn.edu.iuh.fit.repository.CouponRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDetailService {
    private final CouponDetailRepository couponDetailRepository;
    private final CouponDetailTermsRepository couponDetailTermsRepository;
    private final CouponRepository couponRepository;
    private final SeatTypeService seatTypeService;
    private final CouponDuplicateValidator duplicateValidator;

    public List<CouponDetailResponse> getCouponDetails(Integer couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new ResourceNotFoundException("Coupon không tồn tại");
        }

        List<CouponDetail> details = couponDetailRepository.findByCouponIdOrderByLinePriorityAsc(couponId);
        return details.stream()
                .map(this::buildCouponDetailResponse)
                .collect(Collectors.toList());
    }

    public CouponDetail getCouponDetailById(Integer detailId) {
        return couponDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại"));
    }

    @Transactional
    public CouponDetailResponse createCouponDetail(Integer couponId, UpsertCouponDetailRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Validate business rules
        validateCouponDetailRequest(request);

        CouponDetail detail = CouponDetail.builder()
                .coupon(coupon)  // Use relationship instead of couponId
                .enabled(request.getEnabled())
                .targetType(request.getTargetType())
                .targetRefId(request.getTargetRefId())
                .benefitType(request.getBenefitType())
                .lineMaxDiscount(request.getLineMaxDiscount())
                .minQuantity(request.getMinQuantity())
                .minOrderTotal(request.getMinOrderTotal())
                .detailUsageLimit(request.getDetailUsageLimit())
                .linePriority(request.getLinePriority())
                .selectionStrategy(request.getSelectionStrategy())
                .notes(request.getNotes())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        // Save detail first to get ID for terms
        CouponDetail savedDetail = couponDetailRepository.save(detail);

        // Create and save terms if provided
        if (request.getTerms() != null) {
            CouponDetailTerms terms = CouponDetailTerms.builder()
                    .couponDetail(savedDetail)
                    .percent(request.getTerms().getPercent())
                    .amount(request.getTerms().getAmount())
                    .giftServiceId(request.getTerms().getGiftServiceId())
                    .giftQuantity(request.getTerms().getGiftQuantity())
                    .limitQuantityApplied(request.getTerms().getLimitQuantityApplied())
                    .detailUsedCount(0)
                    .build();

            couponDetailTermsRepository.save(terms);
            savedDetail.setTerms(terms);
        }

        // Validate no duplicate ORDER + DISCOUNT_PERCENT details
        try {
            // Create temp detail with explicit fields for validation
            CouponDetail tempDetail = CouponDetail.builder()
                    .targetType(request.getTargetType())
                    .benefitType(request.getBenefitType())
                    .lineMaxDiscount(request.getLineMaxDiscount())
                    .minOrderTotal(request.getMinOrderTotal())
                    .build();
            
            duplicateValidator.validateNoDuplicateOrderDiscountPercent(couponId, tempDetail, null);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        return buildCouponDetailResponse(savedDetail);
    }

    @Transactional
    public CouponDetailResponse updateCouponDetail(Integer detailId, UpsertCouponDetailRequest request) {
        CouponDetail detail = couponDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại"));

        // Get coupon ID directly from repository to avoid Hibernate proxy issues
        Integer couponId = couponDetailRepository.findCouponIdByDetailId(detailId);
        
        // Get parent coupon for date validation
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Validate business rules
        validateCouponDetailRequest(request);
        
        // Get existing terms to check usage count
        CouponDetailTerms existingTerms = couponDetailTermsRepository.findByCouponDetailId(detailId);
        Integer currentUsedCount = existingTerms != null ? existingTerms.getDetailUsedCount() : 0;
        
        // Kiểm tra nếu đã có usage thì không được giảm usage limit xuống dưới used count
        if (request.getDetailUsageLimit() != null && 
            request.getDetailUsageLimit() < currentUsedCount) {
            throw new BadRequestException(ValidationMessages.CANNOT_REDUCE_USAGE_LIMIT);
        }

        // Validate no duplicate ORDER + DISCOUNT_PERCENT details BEFORE setting new values
        try {
            // Create temp detail with explicit fields for validation
            CouponDetail tempDetail = CouponDetail.builder()
                    .targetType(request.getTargetType())
                    .benefitType(request.getBenefitType())
                    .percent(request.getPercent())
                    .lineMaxDiscount(request.getLineMaxDiscount())
                    .minOrderTotal(request.getMinOrderTotal())
                    .build();
            
            duplicateValidator.validateNoDuplicateOrderDiscountPercent(couponId, tempDetail, detailId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Cannot update coupon detail: " + e.getMessage());
        }

        // Now set the new values
        detail.setEnabled(request.getEnabled());
        detail.setTargetType(request.getTargetType());
        detail.setTargetRefId(request.getTargetRefId());
        detail.setBenefitType(request.getBenefitType());
        detail.setLineMaxDiscount(request.getLineMaxDiscount());
        detail.setMinQuantity(request.getMinQuantity());
        detail.setMinOrderTotal(request.getMinOrderTotal());
        detail.setDetailUsageLimit(request.getDetailUsageLimit());
        detail.setLinePriority(request.getLinePriority());
        detail.setSelectionStrategy(request.getSelectionStrategy());
        detail.setNotes(request.getNotes());
        detail.setStartDate(request.getStartDate());
        detail.setEndDate(request.getEndDate());

        CouponDetail savedDetail = couponDetailRepository.save(detail);

        // Update or create terms
        if (request.getTerms() != null) {
            if (existingTerms != null) {
                // Update existing terms
                existingTerms.setPercent(request.getTerms().getPercent());
                existingTerms.setAmount(request.getTerms().getAmount());
                existingTerms.setGiftServiceId(request.getTerms().getGiftServiceId());
                existingTerms.setGiftQuantity(request.getTerms().getGiftQuantity());
                existingTerms.setLimitQuantityApplied(request.getTerms().getLimitQuantityApplied());
                couponDetailTermsRepository.save(existingTerms);
                savedDetail.setTerms(existingTerms);
            } else {
                // Create new terms
                CouponDetailTerms newTerms = CouponDetailTerms.builder()
                        .couponDetail(savedDetail)
                        .percent(request.getTerms().getPercent())
                        .amount(request.getTerms().getAmount())
                        .giftServiceId(request.getTerms().getGiftServiceId())
                        .giftQuantity(request.getTerms().getGiftQuantity())
                        .limitQuantityApplied(request.getTerms().getLimitQuantityApplied())
                        .detailUsedCount(0)
                        .build();
                couponDetailTermsRepository.save(newTerms);
                savedDetail.setTerms(newTerms);
            }
        }
        return buildCouponDetailResponse(savedDetail);
    }

    @Transactional
    public void deleteCouponDetail(Integer detailId) {
        CouponDetail detail = couponDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại"));

        if (detail.getDetailUsedCount() > 0) {
            throw new BadRequestException(ValidationMessages.CANNOT_DELETE_USED_DETAIL);
        }

        couponDetailRepository.delete(detail);
    }

    @Transactional
    public CouponDetailResponse duplicateCouponDetail(Integer detailId) {
        CouponDetail originalDetail = couponDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại"));

        // Get coupon ID directly from repository to avoid Hibernate proxy issues
        Integer couponId = couponDetailRepository.findCouponIdByDetailId(detailId);

        CouponDetail duplicatedDetail = CouponDetail.builder()
                .coupon(originalDetail.getCoupon())  // Use relationship instead of couponId
                .enabled(false) // Mặc định tắt khi duplicate
                .targetType(originalDetail.getTargetType())
                .targetRefId(originalDetail.getTargetRefId())
                .benefitType(originalDetail.getBenefitType())
                .percent(originalDetail.getPercent())
                .amount(originalDetail.getAmount())
                .giftServiceId(originalDetail.getGiftServiceId())
                .giftQuantity(originalDetail.getGiftQuantity())
                .lineMaxDiscount(originalDetail.getLineMaxDiscount())
                .minQuantity(originalDetail.getMinQuantity())
                .limitQuantityApplied(originalDetail.getLimitQuantityApplied())
                .minOrderTotal(originalDetail.getMinOrderTotal())
                .detailUsageLimit(originalDetail.getDetailUsageLimit())
                .detailUsedCount(0)
                .linePriority(originalDetail.getLinePriority() + 1) // Đặt priority sau original
                .selectionStrategy(originalDetail.getSelectionStrategy())
                .notes(originalDetail.getNotes() + " (Copy)")
                .startDate(originalDetail.getStartDate())
                .endDate(originalDetail.getEndDate())
                .build();

        // Validate no duplicate ORDER + DISCOUNT_PERCENT details
        try {
            // Create temp detail with explicit fields for validation
            CouponDetail tempDetail = CouponDetail.builder()
                    .targetType(duplicatedDetail.getTargetType())
                    .benefitType(duplicatedDetail.getBenefitType())
                    .percent(duplicatedDetail.getPercent())
                    .lineMaxDiscount(duplicatedDetail.getLineMaxDiscount())
                    .minOrderTotal(duplicatedDetail.getMinOrderTotal())
                    .build();
            
            duplicateValidator.validateNoDuplicateOrderDiscountPercent(couponId, tempDetail, null);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Cannot duplicate coupon detail: " + e.getMessage());
        }

        CouponDetail savedDetail = couponDetailRepository.save(duplicatedDetail);
        return buildCouponDetailResponse(savedDetail);
    }

    private void validateCouponDetailRequest(UpsertCouponDetailRequest request) {
        // Validate target type and ref id
        if (request.getTargetType() == TargetType.ORDER && request.getTargetRefId() != null) {
            throw new BadRequestException(ValidationMessages.ORDER_NO_TARGET_REF);
        }
        
        // Validate SEAT_TYPE target ref id
        if (request.getTargetType() == TargetType.SEAT_TYPE) {
            if (request.getTargetRefId() == null) {
                throw new BadRequestException("SEAT_TYPE target type requires a valid seat type ID");
            }
            if (!seatTypeService.isValidSeatTypeId(request.getTargetRefId())) {
                throw new BadRequestException("Invalid seat type ID: " + request.getTargetRefId());
            }
        }
        
        // Validate SERVICE target ref id (assuming you have service validation)
        if (request.getTargetType() == TargetType.SERVICE && request.getTargetRefId() == null) {
            throw new BadRequestException("SERVICE target type requires a valid service ID");
        }

        // Validate benefit type values (now in terms)
        if (request.getTerms() != null) {
            switch (request.getBenefitType()) {
                case DISCOUNT_PERCENT:
                    if (request.getTerms().getPercent() == null || 
                        request.getTerms().getPercent().compareTo(BigDecimal.ZERO) <= 0 || 
                        request.getTerms().getPercent().compareTo(new BigDecimal("100")) > 0) {
                        throw new BadRequestException(ValidationMessages.INVALID_PERCENT_VALUE);
                    }
                    break;
                case DISCOUNT_AMOUNT:
                    if (request.getTerms().getAmount() == null || request.getTerms().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException(ValidationMessages.INVALID_AMOUNT_VALUE);
                    }
                    break;
                case FREE_PRODUCT:
                    if (request.getTerms().getGiftServiceId() == null || request.getTerms().getGiftQuantity() == null || 
                        request.getTerms().getGiftQuantity() <= 0) {
                        throw new BadRequestException(ValidationMessages.INVALID_GIFT_CONFIG);
                    }
                    break;
            }
        }

        // Validate constraints
        if (request.getLineMaxDiscount() != null && request.getLineMaxDiscount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Line max discount phải >= 0");
        }
        if (request.getMinQuantity() != null && request.getMinQuantity() < 0) {
            throw new BadRequestException("Min quantity phải >= 0");
        }
        if (request.getTerms() != null && request.getTerms().getLimitQuantityApplied() != null && request.getTerms().getLimitQuantityApplied() < 0) {
            throw new BadRequestException("Limit quantity applied phải >= 0");
        }
        if (request.getMinOrderTotal() != null && request.getMinOrderTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Min order total phải >= 0");
        }
        if (request.getDetailUsageLimit() != null && request.getDetailUsageLimit() < 0) {
            throw new BadRequestException("Detail usage limit phải >= 0");
        }

        // Warning: limitQuantityApplied < minQuantity
        if (request.getTerms() != null && request.getTerms().getLimitQuantityApplied() != null && request.getMinQuantity() != null &&
            request.getTerms().getLimitQuantityApplied() < request.getMinQuantity()) {
            log.warn("limitQuantityApplied ({}) < minQuantity ({}). This may cause unexpected behavior.", 
                     request.getTerms().getLimitQuantityApplied(), request.getMinQuantity());
            // Có thể throw exception nếu muốn block cứng
            // throw new BadRequestException("Limit quantity applied không thể nhỏ hơn min quantity");
        }

        // Validate date range
        if (request.getStartDate() != null && request.getEndDate() != null &&
            request.getStartDate().after(request.getEndDate())) {
            throw new BadRequestException("Start date phải trước end date");
        }
        
        // Require startDate and endDate
        if (request.getStartDate() == null) {
            throw new BadRequestException("Start date là bắt buộc");
        }
        if (request.getEndDate() == null) {
            throw new BadRequestException("End date là bắt buộc");
        }
    }



    private CouponDetailResponse buildCouponDetailResponse(CouponDetail detail) {
        return CouponDetailResponse.builder()
                .id(detail.getId())
                .couponId(detail.getCouponId())
                .enabled(detail.getEnabled())
                .targetType(detail.getTargetType())
                .targetRefId(detail.getTargetRefId())
                .benefitType(detail.getBenefitType())
                .percent(detail.getPercent())
                .amount(detail.getAmount())
                .giftServiceId(detail.getGiftServiceId())
                .giftQuantity(detail.getGiftQuantity())
                .lineMaxDiscount(detail.getLineMaxDiscount())
                .minQuantity(detail.getMinQuantity())
                .limitQuantityApplied(detail.getLimitQuantityApplied())
                .minOrderTotal(detail.getMinOrderTotal())
                .detailUsageLimit(detail.getDetailUsageLimit())
                .detailUsedCount(detail.getDetailUsedCount())
                .linePriority(detail.getLinePriority())
                .selectionStrategy(detail.getSelectionStrategy())
                .notes(detail.getNotes())
                .startDate(detail.getStartDate())
                .endDate(detail.getEndDate())
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .build();
    }
}