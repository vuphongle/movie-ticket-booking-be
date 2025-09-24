package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.CouponApplyRequest;
import vn.edu.iuh.fit.model.request.CouponPreviewRequest;
import vn.edu.iuh.fit.model.request.UpsertCouponRequest;
import vn.edu.iuh.fit.service.CouponDuplicateValidator;
import vn.edu.iuh.fit.service.CouponPreviewService;
import vn.edu.iuh.fit.service.CouponService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;
    private final CouponPreviewService couponPreviewService;
    private final CouponDuplicateValidator duplicateValidator;

    @GetMapping("/coupons")
    public ResponseEntity<?> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/coupons/coupon-by-code")
    public ResponseEntity<?> getAllCouponsByCOde(@RequestParam String code) {
        return ResponseEntity.ok(couponService.getCouponByCode(code));
    }

    @GetMapping("/admin/coupons")
    public ResponseEntity<?> getAllCouponsAdmin() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/admin/coupons/{id}")
    public ResponseEntity<?> getCoupon(@PathVariable Integer id) {
        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @PostMapping("/admin/coupons")
    public ResponseEntity<?> createCoupon(@Valid @RequestBody UpsertCouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @PutMapping("/admin/coupons/{id}")
    public ResponseEntity<?> updateCoupon(@PathVariable Integer id, @Valid @RequestBody UpsertCouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/admin/coupons/{id}")
    public ResponseEntity<?> deleteCoupon(@PathVariable Integer id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/coupons/{id}/duplicate")
    public ResponseEntity<?> duplicateCoupon(@PathVariable Integer id) {
        return ResponseEntity.ok(couponService.duplicateCoupon(id));
    }

    // Preview & Apply endpoints
    @PostMapping("/coupons/{id}/preview")
    public ResponseEntity<?> previewCoupon(@PathVariable Integer id, @Valid @RequestBody CouponPreviewRequest request) {
        return ResponseEntity.ok(couponPreviewService.previewCoupon(id, request));
    }

    @PostMapping("/coupons/apply")
    public ResponseEntity<?> applyCoupon(@Valid @RequestBody CouponApplyRequest request) {
        return ResponseEntity.ok(couponPreviewService.applyCoupon(request));
    }

    // Validation endpoints for frontend
    @PostMapping("/admin/coupons/{id}/validate-status")
    public ResponseEntity<?> validateCouponStatusActivation(@PathVariable Integer id) {
        try {
            duplicateValidator.validateCouponNoDuplicateOrderDiscountPercent(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
