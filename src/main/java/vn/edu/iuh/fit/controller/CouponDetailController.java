package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.model.request.UpsertCouponDetailRequest;
import vn.edu.iuh.fit.model.response.CouponDetailResponse;
import vn.edu.iuh.fit.service.CouponDetailService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/coupon-details")
@RequiredArgsConstructor
public class CouponDetailController {
    private final CouponDetailService couponDetailService;

    @GetMapping
    public ResponseEntity<List<CouponDetailResponse>> getCouponDetails(
            @RequestParam Integer couponId) {
        List<CouponDetail> details = couponDetailService.getCouponDetailsByCouponId(couponId);
        List<CouponDetailResponse> responses = details.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponDetailResponse> getCouponDetailById(@PathVariable Integer id) {
        CouponDetail detail = couponDetailService.getCouponDetailById(id);
        CouponDetailResponse response = mapToResponse(detail);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CouponDetailResponse> createCouponDetail(
            @Valid @RequestBody UpsertCouponDetailRequest request) {
        CouponDetail detail = mapToEntity(request);
        CouponDetail createdDetail = couponDetailService.createCouponDetail(detail);
        CouponDetailResponse response = mapToResponse(createdDetail);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponDetailResponse> updateCouponDetail(
            @PathVariable Integer id,
            @Valid @RequestBody UpsertCouponDetailRequest request) {
        CouponDetail updatedDetail = mapToEntity(request);
        CouponDetail savedDetail = couponDetailService.updateCouponDetail(id, updatedDetail);
        CouponDetailResponse response = mapToResponse(savedDetail);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCouponDetail(@PathVariable Integer id) {
        couponDetailService.deleteCouponDetail(id);
        
        return ResponseEntity.noContent().build();
    }

    private CouponDetailResponse mapToResponse(CouponDetail detail) {
        return CouponDetailResponse.builder()
                .id(detail.getId())
                .couponId(detail.getCouponId())
                .targetType(detail.getTargetType())
                .giftServiceId(detail.getGiftServiceId())
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .build();
    }

    private CouponDetail mapToEntity(UpsertCouponDetailRequest request) {
        return CouponDetail.builder()
                .couponId(request.getCouponId())
                .targetType(request.getTargetType())
                .giftServiceId(request.getGiftServiceId())
                .build();
    }
}