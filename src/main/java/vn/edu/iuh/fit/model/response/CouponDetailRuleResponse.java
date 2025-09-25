package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponBenefitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponDetailRuleResponse {
    Integer id;
    Integer couponDetailId;
    CouponBenefitType benefitType;
    BigDecimal percent;
    BigDecimal amount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}