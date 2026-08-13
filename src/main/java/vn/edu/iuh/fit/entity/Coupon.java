package vn.edu.iuh.fit.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponKind;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupons")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coupon {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  CouponKind kind = CouponKind.VOUCHER; // Default to VOUCHER for backward compatibility

  @Column(unique = true) // Removed nullable = false to allow null for DISPLAY coupons
  String code;

  @Column(nullable = false)
  String name;

  String description;

  @Column(nullable = false)
  Boolean status;

  @Column(name = "start_date", nullable = false)
  Date startDate;

  @Column(name = "end_date", nullable = false)
  Date endDate;

  @Column(name = "created_at")
  Date createdAt;

  @Column(name = "updated_at")
  Date updatedAt;

  @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonManagedReference
  List<CouponDetail> details = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    // Validate business rules
    validateCouponKindRules();

    if (code != null) {
      code = code.toUpperCase(); // Tự động chuyển code thành uppercase
    }
    createdAt = new Date();
    updatedAt = new Date();
  }

  @PreUpdate
  protected void onUpdate() {
    // Validate business rules
    validateCouponKindRules();

    if (code != null) {
      code = code.toUpperCase(); // Tự động chuyển code thành uppercase
    }
    updatedAt = new Date();
  }

  /** Validate business rules for coupon kind */
  private void validateCouponKindRules() {
    if (kind == CouponKind.VOUCHER && (code == null || code.trim().isEmpty())) {
      throw new IllegalArgumentException("VOUCHER coupon must have a code");
    }
    // DISPLAY coupons can have null code - this is allowed
  }
}
