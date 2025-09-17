package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertCouponRequest {
    @NotEmpty(message = "Code không được để trống")
    String code;

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
