package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.SelectionStrategy;
import vn.edu.iuh.fit.model.enums.TargetType;

import java.math.BigDecimal;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupon_details", indexes = {
    @Index(name = "idx_coupon_detail_coupon_id", columnList = "coupon_id"),
    @Index(name = "idx_coupon_detail_enabled", columnList = "enabled"),
    @Index(name = "idx_coupon_detail_priority", columnList = "line_priority")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_coupon_detail_coupon",
                foreignKeyDefinition = "FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE"))
    Coupon coupon;

    @Column(nullable = false)
    @NotNull
    @Builder.Default
    Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    @NotNull
    TargetType targetType;

    @Column(name = "target_ref_id")
    @Min(value = 1, message = "Target reference ID must be positive when provided")
    Integer targetRefId;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 50)
    @NotNull
    BenefitType benefitType;

    // One-to-one relationship with terms table
    @OneToOne(mappedBy = "couponDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    CouponDetailTerms terms;

    // Điều kiện/giới hạn
    @Column(name = "line_max_discount", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Line max discount must not be negative")
    BigDecimal lineMaxDiscount;

    @Column(name = "min_quantity")
    @Min(value = 0, message = "Min quantity must not be negative")
    Integer minQuantity;

    @Column(name = "min_order_total", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Min order total must not be negative")
    BigDecimal minOrderTotal;

    // Hạn mức theo dòng
    @Column(name = "detail_usage_limit")
    @Min(value = 0, message = "Detail usage limit must not be negative")
    Integer detailUsageLimit; // >= 0, 0 = hết lượt

    // Thứ tự & chọn item
    @Column(name = "line_priority", nullable = false)
    @NotNull
    @Min(value = 1, message = "Line priority must be positive")
    @Builder.Default
    Integer linePriority = 100; // số nhỏ chạy trước, mặc định 100

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_strategy", nullable = false, length = 50)
    @NotNull
    @Builder.Default
    SelectionStrategy selectionStrategy = SelectionStrategy.HIGHEST_PRICE_FIRST;

    @Column(length = 1000)
    String notes;

    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    Date endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    Date updatedAt;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createdAt = now;
        updatedAt = now;
        validateBenefitTypeConstraints();
        // Note: Duplicate validation is handled in service layer to avoid circular dependencies
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        validateBenefitTypeConstraints();
        // Note: Duplicate validation is handled in service layer to avoid circular dependencies
    }

    /**
     * Custom validation for benefit type constraints
     * P1: CHECK theo BenefitType/TargetType
     * Note: Terms validation is handled in CouponDetailTerms entity
     */
    private void validateBenefitTypeConstraints() {
        // Basic validation - detailed validation is in CouponDetailTerms
        if (benefitType == null) {
            throw new IllegalArgumentException("Benefit type is required");
        }
        
        // Terms-specific validation will be handled in CouponDetailTerms entity
        // when the terms object is persisted
    }

    /**
     * Convenience method to get coupon ID
     * Safe access to avoid null pointer exceptions
     */
    public Integer getCouponId() {
        return coupon != null ? coupon.getId() : null;
    }
}