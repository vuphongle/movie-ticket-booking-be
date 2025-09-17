package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.SelectionStrategy;
import vn.edu.iuh.fit.model.enums.TargetType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertCouponDetailRequest {
    
    @NotNull(message = "Enabled không được để trống")
    Boolean enabled;

    @NotNull(message = "Target type không được để trống")
    TargetType targetType;

    Integer targetRefId; // null cho ORDER, id seat_type hoặc id additional_service

    @NotNull(message = "Benefit type không được để trống")
    BenefitType benefitType;

    // Giá trị theo loại benefit
    BigDecimal percent; // 0 < % <= 100
    BigDecimal amount; // > 0
    Integer giftServiceId;
    @PositiveOrZero
    Integer giftQuantity;

    // Điều kiện/giới hạn
    @PositiveOrZero
    BigDecimal lineMaxDiscount; // >= 0
    @PositiveOrZero
    Integer minQuantity; // >= 0
    @PositiveOrZero
    Integer limitQuantityApplied; // >= 0
    @PositiveOrZero
    BigDecimal minOrderTotal; // >= 0

    // Hạn mức theo dòng
    @PositiveOrZero
    Integer detailUsageLimit; // >= 0, 0 = hết lượt

    // Thứ tự & chọn item
    @NotNull(message = "Line priority không được để trống")
    Integer linePriority; // số nhỏ chạy trước

    SelectionStrategy selectionStrategy = SelectionStrategy.HIGHEST_PRICE_FIRST;

    String notes;
}