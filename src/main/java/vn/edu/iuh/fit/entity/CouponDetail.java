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

    // Giá trị theo loại benefit - được validate bởi custom constraint
    @Column(precision = 5, scale = 2)
    @DecimalMin(value = "0.01", message = "Percent must be greater than 0")
    @DecimalMax(value = "100.00", message = "Percent must not exceed 100")
    BigDecimal percent;

    @Column(precision = 15, scale = 2)
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount;

    @Column(name = "gift_service_id")
    @Min(value = 1, message = "Gift service ID must be positive when provided")
    Integer giftServiceId;

    @Column(name = "gift_quantity")
    @Min(value = 1, message = "Gift quantity must be positive when provided")
    Integer giftQuantity;

    // Điều kiện/giới hạn
    @Column(name = "line_max_discount", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Line max discount must not be negative")
    BigDecimal lineMaxDiscount;

    @Column(name = "min_quantity")
    @Min(value = 0, message = "Min quantity must not be negative")
    Integer minQuantity;

    @Column(name = "limit_quantity_applied")
    @Min(value = 0, message = "Limit quantity applied must not be negative")
    Integer limitQuantityApplied;

    @Column(name = "min_order_total", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Min order total must not be negative")
    BigDecimal minOrderTotal;

    // Hạn mức theo dòng
    @Column(name = "detail_usage_limit")
    @Min(value = 0, message = "Detail usage limit must not be negative")
    Integer detailUsageLimit; // >= 0, 0 = hết lượt

    @Column(name = "detail_used_count", nullable = false)
    @NotNull
    @Min(value = 0, message = "Detail used count must not be negative")
    @Builder.Default
    Integer detailUsedCount = 0; // system managed

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
     */
    private void validateBenefitTypeConstraints() {
        switch (benefitType) {
            case DISCOUNT_PERCENT:
                if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0 || percent.compareTo(new BigDecimal("100")) > 0) {
                    throw new IllegalArgumentException("PERCENT benefit type requires percent in range (0, 100]");
                }
                if (amount != null || giftServiceId != null || giftQuantity != null) {
                    throw new IllegalArgumentException("PERCENT benefit type must have amount and gift fields as null");
                }
                break;
                
            case DISCOUNT_AMOUNT:
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("AMOUNT benefit type requires amount > 0");
                }
                if (percent != null || giftServiceId != null || giftQuantity != null) {
                    throw new IllegalArgumentException("AMOUNT benefit type must have percent and gift fields as null");
                }
                break;
                
            case FREE_PRODUCT:
                if (giftServiceId == null || giftServiceId <= 0 || giftQuantity == null || giftQuantity <= 0) {
                    throw new IllegalArgumentException("GIFT benefit type requires giftServiceId and giftQuantity > 0");
                }
                if (percent != null || amount != null) {
                    throw new IllegalArgumentException("GIFT benefit type must have percent and amount as null");
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unknown benefit type: " + benefitType);
        }
    }

    /**
     * Convenience method to get coupon ID
     * Safe access to avoid null pointer exceptions
     */
    public Integer getCouponId() {
        return coupon != null ? coupon.getId() : null;
    }

    /**
     * Convenience method to set coupon by ID
     * Should be used carefully and typically only in data loading scenarios
     */
    public void setCouponId(Integer couponId) {
        if (couponId != null) {
            if (this.coupon == null) {
                this.coupon = new Coupon();
            }
            this.coupon.setId(couponId);
        } else {
            this.coupon = null;
        }
    }
}