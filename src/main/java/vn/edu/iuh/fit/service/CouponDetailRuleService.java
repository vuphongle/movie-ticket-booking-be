package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.CouponDetailRule;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.CouponBenefitType;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponDetailRuleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CouponDetailRuleService {
    private final CouponDetailRuleRepository couponDetailRuleRepository;
    private final CouponDetailRepository couponDetailRepository;

    public List<CouponDetailRule> getRulesByCouponDetailId(Integer couponDetailId) {
        if (!couponDetailRepository.existsById(couponDetailId)) {
            throw new ResourceNotFoundException("Coupon detail not found with id: " + couponDetailId);
        }
        
        return couponDetailRuleRepository.findByCouponDetailId(couponDetailId);
    }

    public CouponDetailRule getRuleById(Integer id) {
        return couponDetailRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail rule not found with id: " + id));
    }

    public CouponDetailRule createRule(CouponDetailRule rule) {
        validateRule(rule);
        
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        
        return couponDetailRuleRepository.save(rule);
    }

    public CouponDetailRule updateRule(Integer id, CouponDetailRule updatedRule) {
        CouponDetailRule existingRule = getRuleById(id);
        
        // Update fields
        existingRule.setCouponDetailId(updatedRule.getCouponDetailId());
        existingRule.setBenefitType(updatedRule.getBenefitType());
        existingRule.setPercent(updatedRule.getPercent());
        existingRule.setAmount(updatedRule.getAmount());
        existingRule.setUpdatedAt(LocalDateTime.now());
        
        validateRule(existingRule);
        
        return couponDetailRuleRepository.save(existingRule);
    }

    public void deleteRule(Integer id) {
        if (!couponDetailRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon detail rule not found with id: " + id);
        }
        
        couponDetailRuleRepository.deleteById(id);
    }

    // VALIDATION methods
    private void validateRule(CouponDetailRule rule) {
        // Validate coupon detail exists
        if (!couponDetailRepository.existsById(rule.getCouponDetailId())) {
            throw new BadRequestException("Coupon detail not found with id: " + rule.getCouponDetailId());
        }
        
        // Validate benefit type and corresponding values
        if (rule.getBenefitType() == CouponBenefitType.DISCOUNT_PERCENT) {
            if (rule.getPercent() == null || rule.getPercent().compareTo(BigDecimal.ZERO) <= 0 
                    || rule.getPercent().compareTo(new BigDecimal("100")) > 0) {
                throw new BadRequestException("Percent must be between 0 and 100 for DISCOUNT_PERCENT benefit");
            }
            if (rule.getAmount() != null) {
                throw new BadRequestException("Amount should be null for DISCOUNT_PERCENT benefit");
            }
        } else if (rule.getBenefitType() == CouponBenefitType.DISCOUNT_AMOUNT) {
            if (rule.getAmount() == null || rule.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Amount must be greater than 0 for DISCOUNT_AMOUNT benefit");
            }
            if (rule.getPercent() != null) {
                throw new BadRequestException("Percent should be null for DISCOUNT_AMOUNT benefit");
            }
        } else {
            throw new BadRequestException("Invalid benefit type: " + rule.getBenefitType());
        }
    }
}