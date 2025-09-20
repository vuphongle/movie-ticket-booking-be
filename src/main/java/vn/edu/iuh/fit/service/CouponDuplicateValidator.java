package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.TargetType;
import vn.edu.iuh.fit.repository.CouponDetailRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Service to validate duplicate ORDER + DISCOUNT_PERCENT coupon details
 * 
 * This validator ensures that within a single coupon, there are no two CouponDetail entries
 * that have the exact same combination of:
 * - targetType = ORDER
 * - benefitType = DISCOUNT_PERCENT
 * - percent (normalized to 2 decimal places)
 * - lineMaxDiscount (exact match, null != 0)
 * - minOrderTotal (exact match, null != 0)
 * 
 * Other fields like limitQuantityApplied, selectionStrategy, minQuantity are ignored
 * as they are not relevant for ORDER-level discounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDuplicateValidator {
    
    private final CouponDetailRepository couponDetailRepository;

    /**
     * Validates a single coupon detail against existing details in the same coupon
     * 
     * @param couponId The coupon ID to check against
     * @param detail The detail to validate (can be new or existing)
     * @param excludeDetailId Optional detail ID to exclude from validation (for updates)
     * @throws IllegalArgumentException if duplicate is found
     */
    public void validateNoDuplicateOrderDiscountPercent(Integer couponId, CouponDetail detail, Integer excludeDetailId) {
        // Only validate ORDER + DISCOUNT_PERCENT combinations
        if (detail.getTargetType() != TargetType.ORDER || detail.getBenefitType() != BenefitType.DISCOUNT_PERCENT) {
            return;
        }

        // Get all existing details for this coupon
        List<CouponDetail> existingDetails = couponDetailRepository.findByCouponIdOrderByLinePriorityAsc(couponId);
        
        // Check for duplicates
        for (CouponDetail existing : existingDetails) {
            // Skip self when updating
            if (excludeDetailId != null && Objects.equals(existing.getId(), excludeDetailId)) {
                continue;
            }
            
            // Skip non-ORDER or non-DISCOUNT_PERCENT details
            if (existing.getTargetType() != TargetType.ORDER || existing.getBenefitType() != BenefitType.DISCOUNT_PERCENT) {
                continue;
            }
            
            // Check if this is a duplicate
            if (isDuplicateOrderDiscountPercent(detail, existing)) {
                String message = "Không thể thêm điều kiện này vì đã tồn tại điều kiện giảm giá theo phần trăm tương tự cho tổng đơn hàng.";
                log.warn("Duplicate ORDER + DISCOUNT_PERCENT coupon detail detected. " +
                    "Existing detail ID: {}, percent: {}, lineMaxDiscount: {}, minOrderTotal: {}",
                    existing.getId(),
                    normalizePercent(existing.getPercent()),
                    existing.getLineMaxDiscount(),
                    existing.getMinOrderTotal());
                throw new IllegalArgumentException(message);
            }
        }
    }

    /**
     * Validates all details in a coupon for duplicates (used for status activation)
     * 
     * @param couponId The coupon ID to validate
     * @throws IllegalArgumentException if any duplicates are found
     */
    public void validateCouponNoDuplicateOrderDiscountPercent(Integer couponId) {
        List<CouponDetail> allDetails = couponDetailRepository.findByCouponIdOrderByLinePriorityAsc(couponId);
        
        // Filter to only ORDER + DISCOUNT_PERCENT details
        List<CouponDetail> orderDiscountDetails = allDetails.stream()
            .filter(detail -> detail.getTargetType() == TargetType.ORDER && 
                            detail.getBenefitType() == BenefitType.DISCOUNT_PERCENT)
            .toList();
        
        // Check each detail against all others
        for (int i = 0; i < orderDiscountDetails.size(); i++) {
            CouponDetail detail1 = orderDiscountDetails.get(i);
            
            for (int j = i + 1; j < orderDiscountDetails.size(); j++) {
                CouponDetail detail2 = orderDiscountDetails.get(j);
                
                if (isDuplicateOrderDiscountPercent(detail1, detail2)) {
                    String message = "Không thể kích hoạt coupon này vì tồn tại nhiều điều kiện giảm giá theo phần trăm tương tự cho tổng đơn hàng.";
                    log.warn("Duplicate ORDER + DISCOUNT_PERCENT coupon details detected. " +
                        "Detail IDs: {} and {}, percent: {}, lineMaxDiscount: {}, minOrderTotal: {}",
                        detail1.getId(),
                        detail2.getId(),
                        normalizePercent(detail1.getPercent()),
                        detail1.getLineMaxDiscount(),
                        detail1.getMinOrderTotal());
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    /**
     * Checks if two ORDER + DISCOUNT_PERCENT details are duplicates
     * 
     * @param detail1 First detail
     * @param detail2 Second detail
     * @return true if they are duplicates
     */
    private boolean isDuplicateOrderDiscountPercent(CouponDetail detail1, CouponDetail detail2) {
        // Both should already be ORDER + DISCOUNT_PERCENT, but double-check
        if (detail1.getTargetType() != TargetType.ORDER || detail1.getBenefitType() != BenefitType.DISCOUNT_PERCENT ||
            detail2.getTargetType() != TargetType.ORDER || detail2.getBenefitType() != BenefitType.DISCOUNT_PERCENT) {
            return false;
        }

        // Compare normalized percent values
        BigDecimal percent1 = normalizePercent(detail1.getPercent());
        BigDecimal percent2 = normalizePercent(detail2.getPercent());
        if (percent1.compareTo(percent2) != 0) {
            return false;
        }

        // Compare lineMaxDiscount (null != 0, exact match required)
        if (!isExactMatch(detail1.getLineMaxDiscount(), detail2.getLineMaxDiscount())) {
            return false;
        }

        // Compare minOrderTotal (null != 0, exact match required)
        if (!isExactMatch(detail1.getMinOrderTotal(), detail2.getMinOrderTotal())) {
            return false;
        }

        // If we reach here, they are duplicates
        return true;
    }

    /**
     * Normalizes percent to 2 decimal places for comparison
     * 
     * @param percent The percent value to normalize
     * @return Normalized percent with 2 decimal places
     */
    private BigDecimal normalizePercent(BigDecimal percent) {
        if (percent == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return percent.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if two BigDecimal values are exactly equal (including null handling)
     * null != 0, null == null, 10.00 == 10.0
     * 
     * @param value1 First value
     * @param value2 Second value
     * @return true if they are exactly equal
     */
    private boolean isExactMatch(BigDecimal value1, BigDecimal value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;  // null != 0
        }
        return value1.compareTo(value2) == 0;
    }
}