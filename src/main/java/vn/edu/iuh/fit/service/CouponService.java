package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.constant.ValidationMessages;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.CouponKind;
import vn.edu.iuh.fit.model.request.UpsertCouponRequest;
import vn.edu.iuh.fit.model.response.CouponResponse;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponDetailTermsRepository;
import vn.edu.iuh.fit.repository.CouponRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final CouponDetailRepository couponDetailRepository;
    private final CouponDetailTermsRepository couponDetailTermsRepository;
    private final CouponDuplicateValidator duplicateValidator;

    public List<CouponResponse> getAllCoupons() {
        List<Coupon> coupons = couponRepository.findAll(Sort.by("createdAt").descending());
        return coupons.stream()
                .map(this::buildCouponResponse)
                .collect(Collectors.toList());
    }

    public Coupon getCouponById(Integer id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
    }
    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
    }

    @Transactional
    public Coupon createCoupon(UpsertCouponRequest request) {
        // Enhanced validation
        validateCouponRequest(request);

        // Ràng buộc: Coupon mới tạo luôn phải có status = false
        if (request.getStatus()) {
            throw new BadRequestException("Coupon mới tạo phải có trạng thái ẩn. Sau khi tạo và thêm điều kiện chi tiết, bạn có thể kích hoạt coupon.");
        }

        // Check code duplication only for VOUCHER types with codes
        if (request.getKind() == CouponKind.VOUCHER && request.getCode() != null) {
            if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
                throw new BadRequestException(ValidationMessages.CODE_DUPLICATE);
            }
        }

        Coupon coupon = Coupon.builder()
                .kind(request.getKind())
                .code(request.getCode() != null ? request.getCode().toUpperCase() : null)
                .name(request.getName())
                .description(request.getDescription())
                .status(false) // Force status to false for new coupons
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Created coupon with code: {}", savedCoupon.getCode());
        return savedCoupon;
    }

    @Transactional
    public Coupon updateCoupon(Integer id, UpsertCouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Enhanced validation
        validateCouponRequest(request);

        // Check code duplication only for VOUCHER types with codes
        if (request.getKind() == CouponKind.VOUCHER && request.getCode() != null) {
            String upperCaseCode = request.getCode().toUpperCase();
            if (couponRepository.existsByCode(upperCaseCode) && !upperCaseCode.equals(coupon.getCode())) {
                throw new BadRequestException(ValidationMessages.CODE_DUPLICATE);
            }
        }

        // Validate status activation
        if (request.getStatus() && !hasValidEnabledDetails(id)) {
            throw new BadRequestException(ValidationMessages.CANNOT_ACTIVATE_NO_ENABLED_DETAILS);
        }

        // Validate no duplicate ORDER + DISCOUNT_PERCENT details before activation
        if (request.getStatus()) {
            try {
                duplicateValidator.validateCouponNoDuplicateOrderDiscountPercent(id);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Cannot activate coupon: " + e.getMessage());
            }
        }

        coupon.setKind(request.getKind());
        coupon.setCode(request.getCode() != null ? request.getCode().toUpperCase() : null);
        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setStatus(request.getStatus());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Updated coupon with code: {}", savedCoupon.getCode());
        return savedCoupon;
    }

    @Transactional
    public void deleteCoupon(Integer id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Kiểm tra xem có detail nào đã được sử dụng không
        Long totalUsedCount = couponDetailTermsRepository.sumUsedCountByCouponId(id);
        if (totalUsedCount > 0) {
            throw new BadRequestException(ValidationMessages.CANNOT_DELETE_USED_COUPON);
        }

        // Xóa theo đúng thứ tự: CouponDetailTerms → CouponDetails → Coupon
        // Xóa tất cả terms trước
        couponDetailTermsRepository.deleteByCouponId(id);
        // Xóa tất cả details
        couponDetailRepository.deleteByCouponId(id);
        // Xóa coupon
        couponRepository.delete(coupon);
        log.info("Deleted coupon with code: {}", coupon.getCode());
    }

    @Transactional
    public Coupon duplicateCoupon(Integer id) {
        Coupon originalCoupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Tạo code mới bằng cách thêm timestamp
        String newCode = originalCoupon.getCode() + "_COPY_" + System.currentTimeMillis();

        Coupon duplicatedCoupon = Coupon.builder()
                .code(newCode)
                .name(originalCoupon.getName() + " (Copy)")
                .description(originalCoupon.getDescription())
                .status(false) // Mặc định tắt sau khi duplicate
                .startDate(originalCoupon.getStartDate())
                .endDate(originalCoupon.getEndDate())
                .build();

        Coupon savedCoupon = couponRepository.save(duplicatedCoupon);
        log.info("Duplicated coupon from {} to {}", originalCoupon.getCode(), savedCoupon.getCode());
        return savedCoupon;
    }

    private void validateCouponRequest(UpsertCouponRequest request) {
        // Validate time range
        if (request.getStartDate().after(request.getEndDate()) ||
                request.getStartDate().equals(request.getEndDate())) {
            throw new BadRequestException(ValidationMessages.INVALID_TIME_RANGE);
        }
        
        // Validate CouponKind business rules
        if (request.getKind() == CouponKind.VOUCHER) {
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                throw new BadRequestException("VOUCHER coupon must have a code");
            }
        }
        // DISPLAY coupons can have null code - this is allowed
    }

    private boolean hasValidEnabledDetails(Integer couponId) {
        Long enabledCount = couponDetailRepository.countEnabledDetailsByCouponId(couponId);
        return enabledCount != null && enabledCount > 0;
    }

    private CouponResponse buildCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .kind(coupon.getKind())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .status(coupon.getStatus())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
