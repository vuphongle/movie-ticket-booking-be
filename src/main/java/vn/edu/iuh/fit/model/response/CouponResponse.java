package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.CouponKind;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponResponse {
    Integer id;
    CouponKind kind;
    String code;
    String name;
    String description;
    Boolean status;
    Date startDate;
    Date endDate;
    Date createdAt;
    Date updatedAt;
}