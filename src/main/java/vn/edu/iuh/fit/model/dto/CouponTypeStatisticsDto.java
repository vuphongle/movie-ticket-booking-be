package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** DTO for statistics by coupon type (benefit type and target type) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponTypeStatisticsDto {
  String type; // DISCOUNT_PERCENT, DISCOUNT_AMOUNT, FREE_PRODUCT, or target types
  String category; // "benefit" or "target"
  Integer count; // Number of coupons
  Integer usageCount; // Number of times used
  Integer totalDiscountValue; // Total discount value
  Integer orderCount; // Number of orders
  Double averageDiscount; // Average discount per order
}
