package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.RedemptionConfirmRequest;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponRepository;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedemptionService {
    private final CouponDetailRepository couponDetailRepository;
    private final CouponRepository couponRepository;
    private static final Set<String> processedRedemptions = new HashSet<>(); // Simple in-memory cache - should use Redis in production

    @Transactional
    public void confirmRedemption(RedemptionConfirmRequest request) {
        String redemptionKey = generateRedemptionKey(request.getOrderId(), request.getCouponCode());
        
        // Chống đếm trùng theo orderId + couponCode
        if (processedRedemptions.contains(redemptionKey)) {
            log.warn("Redemption already processed for order {} and coupon {}", request.getOrderId(), request.getCouponCode());
            return; // Already processed, ignore
        }

        // Get coupon by code and validate usage limit at coupon level
        Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại: " + request.getCouponCode()));

        // Check coupon-level usage limit
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("Coupon " + request.getCouponCode() + " đã hết lượt sử dụng");
        }

        // Validate tất cả detail IDs exist
        for (Integer detailId : request.getAppliedDetailIds()) {
            CouponDetail detail = couponDetailRepository.findById(detailId)
                    .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại: " + detailId));
            
            // Verify detail belongs to the coupon
            if (!detail.getCouponId().equals(coupon.getId())) {
                throw new BadRequestException("Coupon detail " + detailId + " không thuộc coupon " + request.getCouponCode());
            }
        }

        // Increase coupon-level used count
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        // Mark as processed
        processedRedemptions.add(redemptionKey);
        log.info("Confirmed redemption for order {} and coupon {}, new used count: {}", 
                request.getOrderId(), request.getCouponCode(), coupon.getUsedCount());
    }

    @Transactional
    public void revertRedemption(RedemptionConfirmRequest request) {
        String redemptionKey = generateRedemptionKey(request.getOrderId(), request.getCouponCode());
        
        // Kiểm tra xem đã được confirm chưa
        if (!processedRedemptions.contains(redemptionKey)) {
            log.warn("Redemption not found for order {} and coupon {}", request.getOrderId(), request.getCouponCode());
            return; // Nothing to revert
        }

        // Get coupon and decrease used count
        Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon không tồn tại: " + request.getCouponCode()));

        // Only decrease if used count > 0
        if (coupon.getUsedCount() > 0) {
            coupon.setUsedCount(coupon.getUsedCount() - 1);
            couponRepository.save(coupon);
            
            log.info("Decreased used count for coupon {} to {}", request.getCouponCode(), coupon.getUsedCount());
        }

        // Remove from processed list
        processedRedemptions.remove(redemptionKey);
        log.info("Reverted redemption for order {} and coupon {}", request.getOrderId(), request.getCouponCode());
    }

    private String generateRedemptionKey(Integer orderId, String couponCode) {
        return orderId + "_" + couponCode;
    }
}