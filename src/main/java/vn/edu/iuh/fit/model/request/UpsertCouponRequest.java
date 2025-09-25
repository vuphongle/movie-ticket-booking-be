package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponType;
import vn.edu.iuh.fit.model.enums.CouponStatus;
import vn.edu.iuh.fit.model.enums.CouponStackingPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertCouponRequest {
    // Type is required - determines if code is needed
    @NotNull(message = "Type không được để trống")
    CouponType type;

    // Code is nullable - PROMOTION=null, VOUCHER=required
    String code;

    @NotEmpty(message = "Name không được để trống")
    String name;
    
    String description; // optional

    @NotNull(message = "Status không được để trống")
    CouponStatus status;

    // Only meaningful for PROMOTION type
    Boolean visible = true;

    @NotNull(message = "Start at không được để trống")
    LocalDateTime startAt;

    @NotNull(message = "End at không được để trống")
    LocalDateTime endAt;

    @NotNull(message = "Stacking policy không được để trống")
    CouponStackingPolicy stackingPolicy = CouponStackingPolicy.EXCLUSIVE;

    // Order-level constraints
    BigDecimal orderMinTotal;
    BigDecimal orderMaxDiscount;
    
    // Usage tracking (optional - for updates)
    Integer usageLimit;
}
