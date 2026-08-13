package vn.edu.iuh.fit.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.AdditionalServiceType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertAdditionalServiceRequest {
  @NotBlank(message = "Tên dịch vụ không được để trống")
  String name;

  @NotBlank(message = "Mô tả không được để trống")
  String description;

  String thumbnail;

  @NotNull(message = "Loại dịch vụ không được để trống")
  AdditionalServiceType type; // SINGLE hoặc COMBO

  // Chỉ sử dụng khi type = SINGLE
  Integer productId;

  // Chỉ sử dụng khi type = SINGLE
  Integer defaultQuantity;

  // Chỉ sử dụng khi type = COMBO
  @Valid List<AdditionalServiceItemRequest> items;

  @NotNull(message = "Trạng thái không được để trống")
  Boolean status;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class AdditionalServiceItemRequest {
    @NotNull(message = "Product ID không được để trống")
    Integer productId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    Integer quantity;
  }
}
