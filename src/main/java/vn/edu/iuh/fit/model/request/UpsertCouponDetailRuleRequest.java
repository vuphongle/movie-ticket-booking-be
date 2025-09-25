package vn.edu.iuh.fit.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponBenefitType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpsertCouponDetailRuleRequest {
    
    @NotNull(message = "Coupon detail ID không được để trống")
    Integer couponDetailId;

    @NotNull(message = "Benefit type không được để trống")
    CouponBenefitType benefitType;

    // For DISCOUNT_PERCENT: 0 < percent <= 100
    BigDecimal percent;

    // For DISCOUNT_AMOUNT: amount > 0
    BigDecimal amount;
}