package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.model.request.UpsertCouponDetailRequest;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.service.CouponDetailService;
import vn.edu.iuh.fit.service.CouponDuplicateValidator;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class CouponDetailController {
    private final CouponDetailService couponDetailService;
    private final CouponDuplicateValidator duplicateValidator;
    private final CouponDetailRepository couponDetailRepository;

    // Details endpoints
    @GetMapping("/admin/coupons/{id}/details")
    public ResponseEntity<?> getCouponDetails(@PathVariable Integer id) {
        return ResponseEntity.ok(couponDetailService.getCouponDetails(id));
    }

    @PostMapping("/admin/coupons/{id}/details")
    public ResponseEntity<?> createCouponDetail(@PathVariable Integer id, @Valid @RequestBody UpsertCouponDetailRequest request) {
        return ResponseEntity.ok(couponDetailService.createCouponDetail(id, request));
    }

    @PutMapping("/admin/coupon-details/{detailId}")
    public ResponseEntity<?> updateCouponDetail(@PathVariable Integer detailId, @Valid @RequestBody UpsertCouponDetailRequest request) {
        return ResponseEntity.ok(couponDetailService.updateCouponDetail(detailId, request));
    }

    @DeleteMapping("/admin/coupon-details/{detailId}")
    public ResponseEntity<?> deleteCouponDetail(@PathVariable Integer detailId) {
        couponDetailService.deleteCouponDetail(detailId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/coupon-details/{detailId}/duplicate")
    public ResponseEntity<?> duplicateCouponDetail(@PathVariable Integer detailId) {
        return ResponseEntity.ok(couponDetailService.duplicateCouponDetail(detailId));
    }

    // Validation endpoints for frontend
    @PostMapping("/admin/coupons/{couponId}/details/validate")
    public ResponseEntity<?> validateCouponDetail(@PathVariable Integer couponId, @Valid @RequestBody UpsertCouponDetailRequest request) {
        try {
            // Create a temporary detail object for validation
            CouponDetail tempDetail = CouponDetail.builder()
                    .targetType(request.getTargetType())
                    .benefitType(request.getBenefitType())
                    .percent(request.getPercent())
                    .lineMaxDiscount(request.getLineMaxDiscount())
                    .minOrderTotal(request.getMinOrderTotal())
                    .build();
            
            duplicateValidator.validateNoDuplicateOrderDiscountPercent(couponId, tempDetail, null);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/admin/coupon-details/{detailId}/validate")
    public ResponseEntity<?> validateCouponDetailUpdate(@PathVariable Integer detailId, @Valid @RequestBody UpsertCouponDetailRequest request) {
        try {
            // Get the coupon ID directly from repository to avoid Hibernate proxy issues
            Integer couponId = couponDetailRepository.findCouponIdByDetailId(detailId);
            if (couponId == null) {
                return ResponseEntity.badRequest().body("Coupon detail not found");
            }
            
            // Create a temporary detail object for validation
            CouponDetail tempDetail = CouponDetail.builder()
                    .targetType(request.getTargetType())
                    .benefitType(request.getBenefitType())
                    .percent(request.getPercent())
                    .lineMaxDiscount(request.getLineMaxDiscount())
                    .minOrderTotal(request.getMinOrderTotal())
                    .build();
            
            duplicateValidator.validateNoDuplicateOrderDiscountPercent(couponId, tempDetail, detailId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}