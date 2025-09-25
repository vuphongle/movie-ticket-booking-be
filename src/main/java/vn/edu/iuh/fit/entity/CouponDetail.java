package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponTargetType;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupon_details", indexes = {
    @Index(name = "idx_coupon_detail_coupon_id", columnList = "coupon_id"),
    @Index(name = "idx_coupon_detail_target_type", columnList = "target_type"),
    @Index(name = "idx_coupon_detail_gift_service", columnList = "gift_service_id")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "coupon_id", nullable = false)
    @NotNull
    Integer couponId;

    // Đối tượng áp dụng: ORDER, PRODUCT, CATEGORY
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    @NotNull
    CouponTargetType targetType;

    // ID tham chiếu đối tượng (nullable cho ORDER target)
    @Column(name = "gift_service_id")
    @Min(value = 1, message = "Gift service ID must be positive when provided")
    Integer giftServiceId;

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
     * Validate all business invariants for CouponDetail entity
     * 1. Target type constraints
     * 2. Required field validations
     */
    private void validateBusinessInvariants() {
        // Rule: target_type=ORDER ⇒ gift_service_id=NULL
        if (targetType == CouponTargetType.ORDER && giftServiceId != null) {
            throw new IllegalArgumentException("ORDER target type must have giftServiceId as null");
        }
        
        // Rule: target_type≠ORDER ⇒ gift_service_id bắt buộc có & > 0
        if (targetType != CouponTargetType.ORDER) {
            if (giftServiceId == null) {
                throw new IllegalArgumentException(targetType + " target type must have giftServiceId specified");
            }
            if (giftServiceId <= 0) {
                throw new IllegalArgumentException(targetType + " target type must have giftServiceId > 0");
            }
        }
        
        // Rule: target_type required
        if (targetType == null) {
            throw new IllegalArgumentException("target_type cannot be null");
        }
        
        // Rule: couponId required
        if (couponId == null) {
            throw new IllegalArgumentException("couponId cannot be null");
        }
    }
}