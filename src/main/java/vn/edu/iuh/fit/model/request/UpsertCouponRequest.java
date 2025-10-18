package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponKind;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertCouponRequest {
  @NotNull(message = "Kind không được để trống")
  CouponKind kind = CouponKind.VOUCHER; // Default to VOUCHER

  String code; // Optional, required only for VOUCHER type

  @NotEmpty(message = "Name không được để trống")
  String name;

  String description; // optional

  @NotNull(message = "Status không được để trống")
  Boolean status;

  @NotNull(message = "Start at không được để trống")
  Date startDate;

  @NotNull(message = "End at không được để trống")
  Date endDate;
}
