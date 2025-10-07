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
 * Service to validate duplicate TICKET + DISCOUNT_PERCENT coupon details
 * 
 * This validator ensures that within a single coupon, there are no two CouponDetail entries
 * that have the exact same combination of:
 * - targetType = TICKET
 * - benefitType = DISCOUNT_PERCENT  
 * - percent (normalized to 2 decimal places)
 * 
 * Other fields are ignored for simplicity after cleanup.
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
        // Only validate TICKET + DISCOUNT_PERCENT combinations
        if (detail.getTargetType() != TargetType.TICKET || detail.getBenefitType() != BenefitType.DISCOUNT_PERCENT) {
            return;
        }

        // Get all existing details for this coupon
        List<CouponDetail> existingDetails = couponDetailRepository.findByCouponIdOrderByIdAsc(couponId);
        
        // Check for duplicates
        for (CouponDetail existing : existingDetails) {
            // Skip self when updating
            if (excludeDetailId != null && Objects.equals(existing.getId(), excludeDetailId)) {
                continue;
            }
            
            // Skip non-TICKET or non-DISCOUNT_PERCENT details
            if (existing.getTargetType() != TargetType.TICKET || existing.getBenefitType() != BenefitType.DISCOUNT_PERCENT) {
                continue;
            }
            
            // Check if this is a duplicate
            if (isDuplicateOrderDiscountPercent(detail, existing)) {
                String message = "Không thể thêm điều kiện này vì đã tồn tại điều kiện giảm giá theo phần trăm tương tự cho vé.";
                log.warn("Duplicate TICKET + DISCOUNT_PERCENT coupon detail detected. " +
                    "Existing detail ID: {}, percent: {}",
                    existing.getId(),
                    existing.getTerms() != null ? normalizePercent(existing.getTerms().getPercent()) : null);
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
        List<CouponDetail> allDetails = couponDetailRepository.findByCouponIdOrderByIdAsc(couponId);
        
        // Filter to only TICKET + DISCOUNT_PERCENT details
        List<CouponDetail> ticketDiscountDetails = allDetails.stream()
            .filter(detail -> detail.getTargetType() == TargetType.TICKET && 
                            detail.getBenefitType() == BenefitType.DISCOUNT_PERCENT)
            .toList();
        
        // Check each detail against all others
        for (int i = 0; i < ticketDiscountDetails.size(); i++) {
            CouponDetail detail1 = ticketDiscountDetails.get(i);
            
            for (int j = i + 1; j < ticketDiscountDetails.size(); j++) {
                CouponDetail detail2 = ticketDiscountDetails.get(j);
                
                if (isDuplicateOrderDiscountPercent(detail1, detail2)) {
                    String message = "Coupon không thể được kích hoạt vì có các điều kiện giảm giá theo phần trăm trùng lặp cho vé.";
                    log.warn("Duplicate TICKET + DISCOUNT_PERCENT coupon details found in coupon {}. " +
                        "Detail IDs: {} and {}, percent: {}",
                        couponId,
                        detail1.getId(),
                        detail2.getId(),
                        detail1.getTerms() != null ? normalizePercent(detail1.getTerms().getPercent()) : null);
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    /**
     * Checks if two TICKET + DISCOUNT_PERCENT details are duplicates
     * 
     * @param detail1 First detail
     * @param detail2 Second detail
     * @return true if they are duplicates
     */
    private boolean isDuplicateOrderDiscountPercent(CouponDetail detail1, CouponDetail detail2) {
        // Both should already be TICKET + DISCOUNT_PERCENT, but double-check
        if (detail1.getTargetType() != TargetType.TICKET || detail1.getBenefitType() != BenefitType.DISCOUNT_PERCENT ||
            detail2.getTargetType() != TargetType.TICKET || detail2.getBenefitType() != BenefitType.DISCOUNT_PERCENT) {
            return false;
        }

        // Compare normalized percent values
        BigDecimal percent1 = detail1.getTerms() != null ? normalizePercent(detail1.getTerms().getPercent()) : null;
        BigDecimal percent2 = detail2.getTerms() != null ? normalizePercent(detail2.getTerms().getPercent()) : null;
        
        if (percent1 == null || percent2 == null || percent1.compareTo(percent2) != 0) {
            return false;
        }

        // If we reach here, they are duplicates (simplified comparison)
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