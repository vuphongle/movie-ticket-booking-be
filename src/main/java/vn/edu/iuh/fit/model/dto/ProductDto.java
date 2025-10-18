package vn.edu.iuh.fit.model.dto;

import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDto {
  Integer id;
  String sku;
  String name;
  String description;
  String unit;
  Integer quantity;
  String thumbnail;
  Boolean status;
  Date createdAt;
  Date updatedAt;
}
