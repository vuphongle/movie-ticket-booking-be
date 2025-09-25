package vn.edu.iuh.fit.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponTargetType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpsertCouponDetailRequest {
    
    @NotNull(message = "Coupon ID không được để trống")
    Integer couponId;

    @NotNull(message = "Target type không được để trống") 
    CouponTargetType targetType;

    // ID tham chiếu cho target (movie_id, product_id, cinema_id...)
    // null for ORDER target, required for others
    Integer giftServiceId;
}