package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.CouponDetailRule;
import vn.edu.iuh.fit.model.enums.CouponBenefitType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponDetailRuleRepository extends JpaRepository<CouponDetailRule, Integer> {
    
    // Find rules by coupon detail ID  
    List<CouponDetailRule> findByCouponDetailId(Integer couponDetailId);
    
    // Check if rule exists for a coupon detail
    boolean existsByCouponDetailId(Integer couponDetailId);
    
    // Find by benefit type
    List<CouponDetailRule> findByBenefitType(CouponBenefitType benefitType);
    
    // Find rules with percent discount
    @Query("SELECT cdr FROM CouponDetailRule cdr WHERE cdr.percent IS NOT NULL")
    List<CouponDetailRule> findRulesWithPercent();
    
    // Find rules with amount discount  
    @Query("SELECT cdr FROM CouponDetailRule cdr WHERE cdr.amount IS NOT NULL")
    List<CouponDetailRule> findRulesWithAmount();
    
    // Find rules by percent range
    @Query("SELECT cdr FROM CouponDetailRule cdr WHERE cdr.percent >= :minPercent AND cdr.percent <= :maxPercent")
    List<CouponDetailRule> findByPercentRange(@Param("minPercent") BigDecimal minPercent, 
                                              @Param("maxPercent") BigDecimal maxPercent);
    
    // Find rules by amount range
    @Query("SELECT cdr FROM CouponDetailRule cdr WHERE cdr.amount >= :minAmount AND cdr.amount <= :maxAmount")
    List<CouponDetailRule> findByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                             @Param("maxAmount") BigDecimal maxAmount);
    
    // Delete rule by coupon detail ID
    @Modifying
    @Transactional
    @Query("DELETE FROM CouponDetailRule cdr WHERE cdr.couponDetailId = :couponDetailId")
    void deleteByCouponDetailId(@Param("couponDetailId") Integer couponDetailId);
    
    // Delete rules for multiple coupon details (batch operation)
    @Modifying
    @Transactional
    @Query("DELETE FROM CouponDetailRule cdr WHERE cdr.couponDetailId IN :couponDetailIds")
    void deleteByCouponDetailIds(@Param("couponDetailIds") List<Integer> couponDetailIds);
    
    // Get coupon detail IDs with rules (for validation)
    @Query("SELECT DISTINCT cdr.couponDetailId FROM CouponDetailRule cdr")
    List<Integer> findAllCouponDetailIdsWithRules();
}