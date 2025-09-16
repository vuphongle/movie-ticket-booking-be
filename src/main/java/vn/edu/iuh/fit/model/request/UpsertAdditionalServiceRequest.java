package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertAdditionalServiceRequest {
    @NotBlank(message = "Tên combo-nước không được để trống")
    String name;
    
    @NotBlank(message = "Mô tả không được để trống")
    String description;
    
    @NotNull(message = "Giá tiền không được để trống")
    @Positive(message = "Giá tiền phải lớn hơn 0")
    Integer price;
    
    String thumbnail;
    
    @NotNull(message = "Trạng thái không được để trống")
    Boolean status;
}