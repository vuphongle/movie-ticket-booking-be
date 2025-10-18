package vn.edu.iuh.fit.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.SelectionStrategy;
import vn.edu.iuh.fit.model.enums.TargetType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpsertCouponDetailRequest {

  @NotNull(message = "Enabled không được để trống")
  Boolean enabled;

  @NotNull(message = "Target type không được để trống")
  TargetType targetType;

  Integer targetRefId; // null cho ORDER, id seat_type hoặc id additional_service

  @NotNull(message = "Benefit type không được để trống")
  BenefitType benefitType;

  // Terms data (will be saved to CouponDetailTerms table)
  TermsData terms;

  // Điều kiện/giới hạn (remaining in CouponDetail)
  @PositiveOrZero BigDecimal lineMaxDiscount; // >= 0
  @PositiveOrZero Integer minQuantity; // >= 0
  @PositiveOrZero BigDecimal minOrderTotal; // >= 0

  // Hạn mức theo dòng (remaining in CouponDetail)
  @PositiveOrZero Integer detailUsageLimit; // >= 0, 0 = hết lượt

  SelectionStrategy selectionStrategy = SelectionStrategy.HIGHEST_PRICE_FIRST;

  String notes;

  // Inner class for terms data
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class TermsData {
    // Benefit values (moved from CouponDetail)
    BigDecimal percent; // 0 < % <= 100
    BigDecimal amount; // > 0
    Integer giftServiceId;
    @PositiveOrZero Integer giftQuantity;

    // Limit conditions (moved from CouponDetail)
    @PositiveOrZero Integer limitQuantityApplied; // >= 0
  }
}
