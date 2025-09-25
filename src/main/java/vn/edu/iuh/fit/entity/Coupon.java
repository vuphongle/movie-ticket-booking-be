package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponType;
import vn.edu.iuh.fit.model.enums.CouponStatus;
import vn.edu.iuh.fit.model.enums.CouponStackingPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupon_code", columnList = "code"),
    @Index(name = "idx_coupon_status", columnList = "status"),
    @Index(name = "idx_coupon_type", columnList = "type"),
    @Index(name = "idx_coupon_visible", columnList = "visible"),
    @Index(name = "idx_coupon_dates", columnList = "start_at, end_at")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    // Loại coupon: PROMOTION (code=null) hoặc VOUCHER (code=not null & unique)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    CouponType type = CouponType.VOUCHER;

    // Code - nullable với constraint unique khi not null
    @Column(unique = true)
    String code;
    
    @Column(nullable = false)
    String name;
    
    String description;
    
    // Status enum thay vì boolean
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    CouponStatus status = CouponStatus.ACTIVE;

    // Chỉ meaningful với PROMOTION - hiển thị cho user chọn hay không
    @Column(nullable = false)
    @Builder.Default
    Boolean visible = true;

    // Đổi tên từ start_date/end_date sang start_at/end_at với LocalDateTime
    @Column(name = "start_at", nullable = false)
    LocalDateTime startAt;

    @Column(name = "end_at", nullable = false) 
    LocalDateTime endAt;

    // Chính sách stacking
    @Enumerated(EnumType.STRING)
    @Column(name = "stacking_policy", nullable = false, length = 20)
    @Builder.Default
    CouponStackingPolicy stackingPolicy = CouponStackingPolicy.EXCLUSIVE;

    // Điều kiện đơn tối thiểu
    @Column(name = "order_min_total", precision = 15, scale = 2)
    BigDecimal orderMinTotal;

    // Trần giảm giá toàn đơn
    @Column(name = "order_max_discount", precision = 15, scale = 2)
    BigDecimal orderMaxDiscount;

    // Usage tracking
    @Column(name = "usage_limit")
    Integer usageLimit;
    
    @Column(name = "used_count", nullable = false)
    @Builder.Default
    Integer usedCount = 0;

    @Column(name = "created_at")
    Date createdAt;

    @Column(name = "updated_at")
    Date updatedAt;

    @PrePersist
    protected void onCreate() {
        if (code != null) {
            code = code.toUpperCase(); // Tự động chuyển code thành uppercase
        }
        
        // Validate business rules
        validateBusinessInvariants();
        
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        if (code != null) {
            code = code.toUpperCase(); // Tự động chuyển code thành uppercase
        }
        
        // Validate business rules
        validateBusinessInvariants();
        
        updatedAt = new Date();
    }
    
    /**
     * Validate all business invariants for Coupon entity
     * 1. Type/Code constraints
     * 2. Date constraints
     * 3. Enum value constraints
     */
    private void validateBusinessInvariants() {
        // Rule: type=PROMOTION ⇒ code=NULL
        if (type == CouponType.PROMOTION && code != null) {
            throw new IllegalArgumentException("PROMOTION type must have code as null");
        }
        
        // Rule: type=VOUCHER ⇒ code phải có & duy nhất
        if (type == CouponType.VOUCHER && code == null) {
            throw new IllegalArgumentException("VOUCHER type must have code as not null");
        }
        
        // Rule: start_at ≤ end_at
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("start_at must be before or equal to end_at");
        }
        
        // Rule: status ∈ {ACTIVE, INACTIVE}
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        
        // Rule: stacking_policy ∈ {STACKABLE, EXCLUSIVE}  
        if (stackingPolicy == null) {
            throw new IllegalArgumentException("stacking_policy cannot be null");
        }
        
        // Rule: type cannot be null
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        
        // Rule: dates cannot be null
        if (startAt == null) {
            throw new IllegalArgumentException("start_at cannot be null");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("end_at cannot be null");
        }
        
        // Rule: BigDecimal validations
        if (orderMinTotal != null && orderMinTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("order_min_total must be >= 0");
        }
        if (orderMaxDiscount != null && orderMaxDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("order_max_discount must be >= 0");
        }
    }
}
