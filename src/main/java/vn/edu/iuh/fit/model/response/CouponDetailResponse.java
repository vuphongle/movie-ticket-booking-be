package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.BenefitType;
import vn.edu.iuh.fit.model.enums.SelectionStrategy;
import vn.edu.iuh.fit.model.enums.TargetType;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponDetailResponse {
    Integer id;
    Integer couponId;
    Boolean enabled;
    TargetType targetType;
    Integer targetRefId;
    BenefitType benefitType;
    
    // Giá trị theo loại benefit
    BigDecimal percent;
    BigDecimal amount;
    Integer giftServiceId;
    Integer giftQuantity;
    
    // Điều kiện/giới hạn
    BigDecimal lineMaxDiscount;
    Integer minQuantity;
    Integer limitQuantityApplied;
    BigDecimal minOrderTotal;
    
    // Hạn mức theo dòng
    Integer detailUsageLimit;
    Integer detailUsedCount;
    
    // Thứ tự & chọn item
    Integer linePriority;
    SelectionStrategy selectionStrategy;
    
    String notes;
    Date startDate;
    Date endDate;
    Date createdAt;
    Date updatedAt;
}