package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertCouponDetailRequest;
import vn.edu.iuh.fit.service.CouponDetailService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class CouponDetailController {
    private final CouponDetailService couponDetailService;

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
}