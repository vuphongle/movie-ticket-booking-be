package vn.edu.iuh.fit.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** DTO for coupon usage history/details */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponUsageDto {
  Integer orderId;
  String couponCode;
  String couponName;
  Integer discountAmount;
  Integer orderTotalBeforeDiscount;
  Integer orderTotalAfterDiscount;
  String customerName;
  String customerEmail;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime usedAt;

  String movieName;
  String cinemaName;
}
