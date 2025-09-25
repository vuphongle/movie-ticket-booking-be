package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponBenefitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupon_detail_rules", indexes = {
    @Index(name = "idx_coupon_detail_rule_detail_id", columnList = "coupon_detail_id")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponDetailRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "coupon_detail_id", nullable = false)
    @NotNull
    Integer couponDetailId;

    // Loại benefit: DISCOUNT_PERCENT hoặc DISCOUNT_AMOUNT
    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 20)
    @NotNull
    CouponBenefitType benefitType;

    // Chỉ 1 trong 2 field này có giá trị, tùy theo benefitType
    @Column(precision = 5, scale = 2)
    @DecimalMin(value = "0.01", message = "Percent must be greater than 0")
    @DecimalMax(value = "100.00", message = "Percent must not exceed 100")
    BigDecimal percent;

    @Column(precision = 15, scale = 2)
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        validateBusinessInvariants();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateBusinessInvariants();
    }

    /**
     * Validate all business invariants for CouponDetailRule entity:
     * 1. Exactly one of percent/amount must be set based on benefitType
     * 2. Values must be positive and within valid ranges
     */
    private void validateBusinessInvariants() {
        if (benefitType == null) {
            throw new IllegalArgumentException("benefitType cannot be null");
        }
        
        if (couponDetailId == null) {
            throw new IllegalArgumentException("couponDetailId cannot be null");
        }
        
        // Rule: benefitType determines which field should be set
        if (benefitType == CouponBenefitType.DISCOUNT_PERCENT) {
            if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0 
                    || percent.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percent must be between 0 and 100 for DISCOUNT_PERCENT benefit");
            }
            if (amount != null) {
                throw new IllegalArgumentException("Amount should be null for DISCOUNT_PERCENT benefit");
            }
        } else if (benefitType == CouponBenefitType.DISCOUNT_AMOUNT) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0 for DISCOUNT_AMOUNT benefit");
            }
            if (percent != null) {
                throw new IllegalArgumentException("Percent should be null for DISCOUNT_AMOUNT benefit");
            }
        } else {
            throw new IllegalArgumentException("Invalid benefit type: " + benefitType);
        }
    }
}