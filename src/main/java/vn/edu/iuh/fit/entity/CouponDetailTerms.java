package vn.edu.iuh.fit.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupon_detail_terms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponDetailTerms {
  @Id Integer id; // Same as coupon_detail.id (1-1 relationship)

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(
      name = "id",
      foreignKey =
          @ForeignKey(
              name = "fk_coupon_detail_terms_coupon_detail",
              foreignKeyDefinition =
                  "FOREIGN KEY (id) REFERENCES coupon_details(id) ON DELETE CASCADE"))
  @JsonBackReference
  CouponDetail couponDetail;

  // Benefit values (migrated from CouponDetail)
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

  // Limit conditions (migrated from CouponDetail)
  @Column(name = "limit_quantity_applied")
  @Min(value = 0, message = "Limit quantity applied must not be negative")
  Integer limitQuantityApplied;

  // Usage tracking (migrated from CouponDetail)
  @Column(name = "detail_used_count", nullable = false)
  @NotNull
  @Min(value = 0, message = "Detail used count must not be negative")
  @Builder.Default
  Integer detailUsedCount = 0; // system managed

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

  /** Convenience method to get coupon detail ID */
  public Integer getCouponDetailId() {
    return couponDetail != null ? couponDetail.getId() : null;
  }
}
