package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.CouponStatus;
import vn.edu.iuh.fit.repository.CouponRepository;
import vn.edu.iuh.fit.repository.CouponDetailRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {
    private final CouponRepository couponRepository;
    private final CouponDetailRepository couponDetailRepository;

    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Coupon getCouponById(Integer id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại với ID: " + id));
    }

    public Coupon createCoupon(Coupon coupon) {
        validateCouponForCreate(coupon);
        Coupon savedCoupon = couponRepository.save(coupon);
        return savedCoupon;
    }

    public Coupon updateCoupon(Integer id, Coupon updatedCoupon) {
        Coupon existingCoupon = getCouponById(id);
        validateCouponForUpdate(existingCoupon, updatedCoupon);
        
        existingCoupon.setCode(updatedCoupon.getCode());
        existingCoupon.setName(updatedCoupon.getName());
        existingCoupon.setDescription(updatedCoupon.getDescription());
        existingCoupon.setStatus(updatedCoupon.getStatus());
        
        Coupon savedCoupon = couponRepository.save(existingCoupon);
        return savedCoupon;
    }

    public void deleteCoupon(Integer id) {
        Coupon coupon = getCouponById(id);
        couponDetailRepository.deleteByCouponId(id);
        couponRepository.delete(coupon);
    }

    private void validateCouponForCreate(Coupon coupon) {
        if (coupon.getCode() != null && couponRepository.existsByCode(coupon.getCode())) {
            throw new BadRequestException("Coupon code đã tồn tại: " + coupon.getCode());
        }
    }

    private void validateCouponForUpdate(Coupon existing, Coupon updated) {
        if (!existing.getCode().equals(updated.getCode()) && 
            couponRepository.existsByCode(updated.getCode())) {
            throw new BadRequestException("Coupon code đã tồn tại: " + updated.getCode());
        }
    }

    public Coupon duplicateCoupon(Integer id) {
        Coupon original = getCouponById(id);
        
        Coupon duplicate = Coupon.builder()
                .type(original.getType())
                .code(generateUniqueCode(original.getCode()))
                .name(original.getName() + " (Copy)")
                .description(original.getDescription())
                .status(CouponStatus.INACTIVE)
                .visible(original.getVisible())
                .startAt(original.getStartAt())
                .endAt(original.getEndAt())
                .stackingPolicy(original.getStackingPolicy())
                .orderMinTotal(original.getOrderMinTotal())
                .orderMaxDiscount(original.getOrderMaxDiscount())
                .usageLimit(original.getUsageLimit())
                .usedCount(0)
                .build();
        
        return couponRepository.save(duplicate);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findActiveCoupons(CouponStatus.ACTIVE, LocalDateTime.now());
    }

    private String generateUniqueCode(String originalCode) {
        String baseCode = originalCode + "_COPY";
        String newCode = baseCode;
        int counter = 1;
        
        while (couponRepository.existsByCode(newCode)) {
            newCode = baseCode + "_" + counter;
            counter++;
        }
        
        return newCode;
    }
}
