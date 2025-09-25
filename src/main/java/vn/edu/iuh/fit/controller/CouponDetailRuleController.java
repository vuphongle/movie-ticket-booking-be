package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.CouponDetailRule;
import vn.edu.iuh.fit.model.request.UpsertCouponDetailRuleRequest;
import vn.edu.iuh.fit.model.response.CouponDetailRuleResponse;
import vn.edu.iuh.fit.service.CouponDetailRuleService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/coupon-detail-rules")
@RequiredArgsConstructor
public class CouponDetailRuleController {
    private final CouponDetailRuleService couponDetailRuleService;

    // GET /admin/coupon-detail-rules?couponDetailId={id} - Get all rules for a coupon detail
    @GetMapping
    public ResponseEntity<List<CouponDetailRuleResponse>> getCouponDetailRules(
            @RequestParam Integer couponDetailId) {
        log.info("GET /admin/coupon-detail-rules?couponDetailId={} - Getting coupon detail rules", 
                couponDetailId);
        
        List<CouponDetailRule> rules = couponDetailRuleService.getRulesByCouponDetailId(couponDetailId);
        List<CouponDetailRuleResponse> responses = rules.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    // GET /admin/coupon-detail-rules/{id} - Get rule by ID
    @GetMapping("/{id}")
    public ResponseEntity<CouponDetailRuleResponse> getCouponDetailRuleById(@PathVariable Integer id) {
        log.info("GET /admin/coupon-detail-rules/{} - Getting coupon detail rule by ID", id);
        
        CouponDetailRule rule = couponDetailRuleService.getRuleById(id);
        CouponDetailRuleResponse response = mapToResponse(rule);
        
        return ResponseEntity.ok(response);
    }

    // POST /admin/coupon-detail-rules - Create new coupon detail rule
    @PostMapping
    public ResponseEntity<CouponDetailRuleResponse> createCouponDetailRule(
            @Valid @RequestBody UpsertCouponDetailRuleRequest request) {
        log.info("POST /admin/coupon-detail-rules - Creating coupon detail rule for couponDetailId: {}", 
                request.getCouponDetailId());
        
        CouponDetailRule rule = mapToEntity(request);
        CouponDetailRule createdRule = couponDetailRuleService.createRule(rule);
        CouponDetailRuleResponse response = mapToResponse(createdRule);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /admin/coupon-detail-rules/{id} - Update coupon detail rule
    @PutMapping("/{id}")
    public ResponseEntity<CouponDetailRuleResponse> updateCouponDetailRule(
            @PathVariable Integer id,
            @Valid @RequestBody UpsertCouponDetailRuleRequest request) {
        log.info("PUT /admin/coupon-detail-rules/{} - Updating coupon detail rule", id);
        
        CouponDetailRule updatedRule = mapToEntity(request);
        CouponDetailRule savedRule = couponDetailRuleService.updateRule(id, updatedRule);
        CouponDetailRuleResponse response = mapToResponse(savedRule);
        
        return ResponseEntity.ok(response);
    }

    // DELETE /admin/coupon-detail-rules/{id} - Delete coupon detail rule
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCouponDetailRule(@PathVariable Integer id) {
        log.info("DELETE /admin/coupon-detail-rules/{} - Deleting coupon detail rule", id);
        
        couponDetailRuleService.deleteRule(id);
        
        return ResponseEntity.noContent().build();
    }

    // MAPPING methods
    private CouponDetailRuleResponse mapToResponse(CouponDetailRule rule) {
        return CouponDetailRuleResponse.builder()
                .id(rule.getId())
                .couponDetailId(rule.getCouponDetailId())
                .benefitType(rule.getBenefitType())
                .percent(rule.getPercent())
                .amount(rule.getAmount())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private CouponDetailRule mapToEntity(UpsertCouponDetailRuleRequest request) {
        return CouponDetailRule.builder()
                .couponDetailId(request.getCouponDetailId())
                .benefitType(request.getBenefitType())
                .percent(request.getPercent())
                .amount(request.getAmount())
                .build();
    }
}