package vn.edu.iuh.fit.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.TargetType;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "coupon_details",
    indexes = {
      @Index(name = "idx_coupon_detail_coupon_id", columnList = "coupon_id"),
      @Index(name = "idx_coupon_detail_enabled", columnList = "enabled")
    })
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponDetail {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "coupon_id",
      nullable = false,
      foreignKey =
          @ForeignKey(
              name = "fk_coupon_detail_coupon",
              foreignKeyDefinition =
                  "FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE"))
  @JsonBackReference
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
  @JsonManagedReference
  CouponDetailTerms terms;

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
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }

  /** Convenience method to get coupon ID Safe access to avoid null pointer exceptions */
  public Integer getCouponId() {
    return coupon != null ? coupon.getId() : null;
  }
}
