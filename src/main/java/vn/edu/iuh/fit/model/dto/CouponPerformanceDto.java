package vn.edu.iuh.fit.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** DTO for individual coupon performance statistics */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponPerformanceDto {
  // Coupon basic info
  Integer couponId;
  String couponCode;
  String couponName;
  String description;
  String kind; // VOUCHER or DISPLAY
  Boolean status;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime startDate;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime endDate;

  // Performance metrics
  Integer usageCount; // Total times used
  Integer uniqueCustomers; // Number of unique customers
  Integer enabledDetailsCount; // Number of active details
  Integer totalDetailsCount; // Total number of details

  // Financial metrics
  Integer totalDiscountValue; // Total discount given
  Integer revenueWithCoupon; // Revenue from orders using this coupon
  Integer revenueBeforeDiscount; // Revenue before applying discount
  Double averageDiscountPerOrder; // Average discount per order
  Double averageOrderValue; // Average order value

  // Efficiency metrics
  Double usageRate; // If has limit: used / limit
  Double conversionRate; // orders with coupon / total orders in period
  Double discountPercentage; // total discount / revenue before discount

  // Time metrics
  Long daysActive; // Number of days the coupon has been active
  Long daysRemaining; // Days until expiration

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime lastUsedAt; // When was it last used

  // Detail breakdown
  Integer discountPercentCount; // Number of DISCOUNT_PERCENT details
  Integer discountAmountCount; // Number of DISCOUNT_AMOUNT details
  Integer freeProductCount; // Number of FREE_PRODUCT details

  // Status classification
  String statusLabel; // "Kích hoạt", "Ẩn", "Sắp có hiệu lực", "Hết hạn"
}
