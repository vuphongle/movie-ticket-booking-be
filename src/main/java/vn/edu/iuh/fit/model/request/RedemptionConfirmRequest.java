package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RedemptionConfirmRequest {
  @NotNull(message = "Order ID không được để trống")
  Integer orderId;

  @NotEmpty(message = "Coupon code không được để trống")
  String couponCode;

  @NotEmpty(message = "Applied detail IDs không được để trống")
  List<Integer> appliedDetailIds;
}
