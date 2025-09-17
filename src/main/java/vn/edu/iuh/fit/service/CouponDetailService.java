package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.constant.ValidationMessages;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.TargetType;
import vn.edu.iuh.fit.model.request.UpsertCouponDetailRequest;
import vn.edu.iuh.fit.model.response.CouponDetailResponse;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDetailService {
    private final CouponDetailRepository couponDetailRepository;
    private final CouponRepository couponRepository;
    private final SeatTypeService seatTypeService;

    public List<CouponDetailResponse> getCouponDetails(Integer couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new ResourceNotFoundException("Coupon không tồn tại");
        }

        List<CouponDetail> details = couponDetailRepository.findByCouponIdOrderByLinePriorityAsc(couponId);
        return details.stream()
                .map(this::buildCouponDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CouponDetail createCouponDetail(Integer couponId, UpsertCouponDetailRequest request) {
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
                .percent(request.getPercent())
                .amount(request.getAmount())
                .giftServiceId(request.getGiftServiceId())
                .giftQuantity(request.getGiftQuantity())
                .lineMaxDiscount(request.getLineMaxDiscount())
                .minQuantity(request.getMinQuantity())
                .limitQuantityApplied(request.getLimitQuantityApplied())
                .minOrderTotal(request.getMinOrderTotal())
                .detailUsageLimit(request.getDetailUsageLimit())
                .detailUsedCount(0)
                .linePriority(request.getLinePriority())
                .selectionStrategy(request.getSelectionStrategy())
                .notes(request.getNotes())
                .build();

        return couponDetailRepository.save(detail);
    }

    @Transactional
    public CouponDetail updateCouponDetail(Integer detailId, UpsertCouponDetailRequest request) {
        CouponDetail detail = couponDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại"));

        // Validate business rules
        validateCouponDetailRequest(request);

        // Kiểm tra nếu đã có usage thì không được giảm usage limit xuống dưới used count
        if (request.getDetailUsageLimit() != null && 
            request.getDetailUsageLimit() < detail.getDetailUsedCount()) {
            throw new BadRequestException(ValidationMessages.CANNOT_REDUCE_USAGE_LIMIT);
        }

        detail.setEnabled(request.getEnabled());
        detail.setTargetType(request.getTargetType());
        detail.setTargetRefId(request.getTargetRefId());
        detail.setBenefitType(request.getBenefitType());
        detail.setPercent(request.getPercent());
        detail.setAmount(request.getAmount());
        detail.setGiftServiceId(request.getGiftServiceId());
        detail.setGiftQuantity(request.getGiftQuantity());
        detail.setLineMaxDiscount(request.getLineMaxDiscount());
        detail.setMinQuantity(request.getMinQuantity());
        detail.setLimitQuantityApplied(request.getLimitQuantityApplied());
        detail.setMinOrderTotal(request.getMinOrderTotal());
        detail.setDetailUsageLimit(request.getDetailUsageLimit());
        detail.setLinePriority(request.getLinePriority());
        detail.setSelectionStrategy(request.getSelectionStrategy());
        detail.setNotes(request.getNotes());

        return couponDetailRepository.save(detail);
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
    public CouponDetail duplicateCouponDetail(Integer detailId) {
        CouponDetail originalDetail = couponDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại"));

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
                .build();

        return couponDetailRepository.save(duplicatedDetail);
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

        // Validate benefit type values
        switch (request.getBenefitType()) {
            case DISCOUNT_PERCENT:
                if (request.getPercent() == null || 
                    request.getPercent().compareTo(BigDecimal.ZERO) <= 0 || 
                    request.getPercent().compareTo(new BigDecimal("100")) > 0) {
                    throw new BadRequestException(ValidationMessages.INVALID_PERCENT_VALUE);
                }
                break;
            case DISCOUNT_AMOUNT:
                if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException(ValidationMessages.INVALID_AMOUNT_VALUE);
                }
                break;
            case FREE_PRODUCT:
                if (request.getGiftServiceId() == null || request.getGiftQuantity() == null || 
                    request.getGiftQuantity() <= 0) {
                    throw new BadRequestException(ValidationMessages.INVALID_GIFT_CONFIG);
                }
                break;
        }

        // Validate constraints
        if (request.getLineMaxDiscount() != null && request.getLineMaxDiscount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Line max discount phải >= 0");
        }
        if (request.getMinQuantity() != null && request.getMinQuantity() < 0) {
            throw new BadRequestException("Min quantity phải >= 0");
        }
        if (request.getLimitQuantityApplied() != null && request.getLimitQuantityApplied() < 0) {
            throw new BadRequestException("Limit quantity applied phải >= 0");
        }
        if (request.getMinOrderTotal() != null && request.getMinOrderTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Min order total phải >= 0");
        }
        if (request.getDetailUsageLimit() != null && request.getDetailUsageLimit() < 0) {
            throw new BadRequestException("Detail usage limit phải >= 0");
        }

        // Warning: limitQuantityApplied < minQuantity
        if (request.getLimitQuantityApplied() != null && request.getMinQuantity() != null &&
            request.getLimitQuantityApplied() < request.getMinQuantity()) {
            log.warn("limitQuantityApplied ({}) < minQuantity ({}). This may cause unexpected behavior.", 
                     request.getLimitQuantityApplied(), request.getMinQuantity());
            // Có thể throw exception nếu muốn block cứng
            // throw new BadRequestException("Limit quantity applied không thể nhỏ hơn min quantity");
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
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .build();
    }
}