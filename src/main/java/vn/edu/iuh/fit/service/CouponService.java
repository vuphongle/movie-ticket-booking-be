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
import vn.edu.iuh.fit.model.request.UpsertCouponRequest;
import vn.edu.iuh.fit.model.response.CouponResponse;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final CouponDetailRepository couponDetailRepository;

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

    @Transactional
    public Coupon createCoupon(UpsertCouponRequest request) {
        // Enhanced validation
        validateCouponRequest(request);
        
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new BadRequestException(ValidationMessages.CODE_DUPLICATE);
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
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
        
        String upperCaseCode = request.getCode().toUpperCase();
        if (couponRepository.existsByCode(upperCaseCode) && !coupon.getCode().equals(upperCaseCode)) {
            throw new BadRequestException(ValidationMessages.CODE_DUPLICATE);
        }

        // Validate status activation
        if (request.getStatus() && !hasValidEnabledDetails(id)) {
            throw new BadRequestException(ValidationMessages.CANNOT_ACTIVATE_NO_ENABLED_DETAILS);
        }

        coupon.setCode(upperCaseCode);
        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setStatus(request.getStatus());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Updated coupon with code: {}", savedCoupon.getCode());
        return savedCoupon;
    }

    @Transactional
    public void deleteCoupon(Integer id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại"));

        // Kiểm tra xem có detail nào đã được sử dụng không
        Long totalUsedCount = couponDetailRepository.sumUsedCountByCouponId(id);
        if (totalUsedCount > 0) {
            throw new BadRequestException(ValidationMessages.CANNOT_DELETE_USED_COUPON);
        }

        // Xóa tất cả details trước
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
                .startAt(originalCoupon.getStartAt())
                .endAt(originalCoupon.getEndAt())
                .build();

        Coupon savedCoupon = couponRepository.save(duplicatedCoupon);
        log.info("Duplicated coupon from {} to {}", originalCoupon.getCode(), savedCoupon.getCode());
        return savedCoupon;
    }

    private void validateCouponRequest(UpsertCouponRequest request) {
        // Validate time range
        if (request.getStartAt().after(request.getEndAt()) || 
            request.getStartAt().equals(request.getEndAt())) {
            throw new BadRequestException(ValidationMessages.INVALID_TIME_RANGE);
        }
    }

    private boolean hasValidEnabledDetails(Integer couponId) {
        Long enabledCount = couponDetailRepository.countEnabledDetailsByCouponId(couponId);
        return enabledCount != null && enabledCount > 0;
    }

    private CouponResponse buildCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .status(coupon.getStatus())
                .startAt(coupon.getStartAt())
                .endAt(coupon.getEndAt())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
