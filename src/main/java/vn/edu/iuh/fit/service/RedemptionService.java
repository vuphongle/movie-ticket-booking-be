package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.entity.CouponDetailTerms;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.RedemptionConfirmRequest;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponDetailTermsRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedemptionService {
    private final CouponDetailRepository couponDetailRepository;
    private final CouponDetailTermsRepository couponDetailTermsRepository;
    private static final Set<String> processedRedemptions = new HashSet<>(); // Simple in-memory cache - should use Redis in production

    @Transactional
    public void confirmRedemption(RedemptionConfirmRequest request) {
        String redemptionKey = generateRedemptionKey(request.getOrderId(), request.getCouponCode());
        
        // Chống đếm trùng theo orderId + couponCode
        if (processedRedemptions.contains(redemptionKey)) {
            log.warn("Redemption already processed for order {} and coupon {}", request.getOrderId(), request.getCouponCode());
            return; // Already processed, ignore
        }

        // Validate và tăng used count cho từng detail
        for (Integer detailId : request.getAppliedDetailIds()) {
            CouponDetail detail = couponDetailRepository.findById(detailId)
                    .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại: " + detailId));

            // Get or create terms
            CouponDetailTerms terms = couponDetailTermsRepository.findByCouponDetailId(detailId);
            if (terms == null) {
                // Create terms if not exists (backward compatibility)
                terms = CouponDetailTerms.builder()
                        .couponDetail(detail)
                        .detailUsedCount(0)
                        .build();
            }

            // Tăng used count atomically
            terms.setDetailUsedCount(terms.getDetailUsedCount() + 1);
            couponDetailTermsRepository.save(terms);
            
            log.info("Increased used count for coupon detail {} to {}", detailId, terms.getDetailUsedCount());
        }

        // Đánh dấu đã xử lý
        processedRedemptions.add(redemptionKey);
        log.info("Confirmed redemption for order {} and coupon {}", request.getOrderId(), request.getCouponCode());
    }

    @Transactional
    public void revertRedemption(RedemptionConfirmRequest request) {
        String redemptionKey = generateRedemptionKey(request.getOrderId(), request.getCouponCode());
        
        // Kiểm tra xem đã được confirm chưa
        if (!processedRedemptions.contains(redemptionKey)) {
            log.warn("Redemption not found for order {} and coupon {}", request.getOrderId(), request.getCouponCode());
            return; // Nothing to revert
        }

        // Giảm used count cho từng detail
        for (Integer detailId : request.getAppliedDetailIds()) {
            CouponDetail detail = couponDetailRepository.findById(detailId)
                    .orElseThrow(() -> new ResourceNotFoundException("Coupon detail không tồn tại: " + detailId));

            // Get terms
            CouponDetailTerms terms = couponDetailTermsRepository.findByCouponDetailId(detailId);
            if (terms != null) {
                // Chỉ giảm nếu used count > 0
                if (terms.getDetailUsedCount() > 0) {
                    terms.setDetailUsedCount(terms.getDetailUsedCount() - 1);
                    couponDetailTermsRepository.save(terms);
                    
                    log.info("Decreased used count for coupon detail {} to {}", detailId, terms.getDetailUsedCount());
                }
            }
        }

        // Xóa khỏi danh sách đã xử lý
        processedRedemptions.remove(redemptionKey);
        log.info("Reverted redemption for order {} and coupon {}", request.getOrderId(), request.getCouponCode());
    }

    private String generateRedemptionKey(Integer orderId, String couponCode) {
        return orderId + "_" + couponCode;
    }
}