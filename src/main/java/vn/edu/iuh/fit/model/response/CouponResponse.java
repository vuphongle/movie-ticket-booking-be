package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponType;
import vn.edu.iuh.fit.model.enums.CouponStatus;
import vn.edu.iuh.fit.model.enums.CouponStackingPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponResponse {
    Integer id;
    CouponType type;
    String code; // nullable for PROMOTION
    String name;
    String description;
    CouponStatus status;
    Boolean visible;
    LocalDateTime startAt;
    LocalDateTime endAt;
    CouponStackingPolicy stackingPolicy;
    BigDecimal orderMinTotal;
    BigDecimal orderMaxDiscount;
    
    // Usage tracking
    Integer usageLimit;
    Integer usedCount;
    
    Date createdAt;
    Date updatedAt;
}