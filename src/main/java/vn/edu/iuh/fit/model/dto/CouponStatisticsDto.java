package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** DTO for overall coupon statistics dashboard */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponStatisticsDto {
  // Basic counts
  Integer totalCoupons;
  Integer activeCoupons;
  Integer upcomingCoupons;
  Integer expiredCoupons;
  Integer hiddenCoupons;

  // Usage statistics
  Integer totalRedemptions;
  Integer uniqueCustomers;
  Integer ordersWithCoupons;
  Integer ordersWithoutCoupons;

  // Financial metrics
  Integer totalDiscountAmount;
  Integer totalRevenueBeforeDiscount;
  Integer totalRevenueAfterDiscount;
  Double averageDiscountPercentage;
  Double averageDiscountPerOrder;
  Double averageOrderValueWithCoupon;
  Double averageOrderValueWithoutCoupon;

  // Efficiency metrics
  Double couponUsageRate; // % of coupons that have been used
  Double orderConversionRate; // % of orders using coupons
  Double discountImpactRate; // discount / revenue before discount
}
