package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.model.enums.CouponStatus;
import vn.edu.iuh.fit.model.request.UpsertCouponRequest;
import vn.edu.iuh.fit.model.response.CouponResponse;
import vn.edu.iuh.fit.service.CouponService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        log.info("GET /admin/coupons - Getting all coupons");
        
        List<Coupon> coupons = couponService.getAllCoupons();
        List<CouponResponse> responses = coupons.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable Integer id) {
        log.info("GET /admin/coupons/{} - Getting coupon by ID", id);
        
        Coupon coupon = couponService.getCouponById(id);
        CouponResponse response = mapToResponse(coupon);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody UpsertCouponRequest request) {
        log.info("POST /admin/coupons - Creating coupon: {}", request.getName());
        
        Coupon coupon = mapToEntity(request);
        Coupon createdCoupon = couponService.createCoupon(coupon);
        CouponResponse response = mapToResponse(createdCoupon);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Integer id,
            @Valid @RequestBody UpsertCouponRequest request) {
        log.info("PUT /admin/coupons/{} - Updating coupon: {}", id, request.getName());
        
        Coupon updatedCoupon = mapToEntity(request);
        Coupon savedCoupon = couponService.updateCoupon(id, updatedCoupon);
        CouponResponse response = mapToResponse(savedCoupon);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Integer id) {
        log.info("DELETE /admin/coupons/{} - Deleting coupon", id);
        
        couponService.deleteCoupon(id);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<CouponResponse> duplicateCoupon(@PathVariable Integer id) {
        Coupon duplicatedCoupon = couponService.duplicateCoupon(id);
        CouponResponse response = mapToResponse(duplicatedCoupon);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<CouponResponse>> getActiveCoupons() {
        List<Coupon> activeCoupons = couponService.getActiveCoupons();
        List<CouponResponse> responses = activeCoupons.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .type(coupon.getType())
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .status(coupon.getStatus())
                .visible(coupon.getVisible())
                .startAt(coupon.getStartAt())
                .endAt(coupon.getEndAt())
                .stackingPolicy(coupon.getStackingPolicy())
                .orderMinTotal(coupon.getOrderMinTotal())
                .orderMaxDiscount(coupon.getOrderMaxDiscount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    private Coupon mapToEntity(UpsertCouponRequest request) {
        return Coupon.builder()
                .type(request.getType())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .visible(request.getVisible())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .stackingPolicy(request.getStackingPolicy())
                .orderMinTotal(request.getOrderMinTotal())
                .orderMaxDiscount(request.getOrderMaxDiscount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .build();
    }
}