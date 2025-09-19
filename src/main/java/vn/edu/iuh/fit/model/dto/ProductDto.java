package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

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
    String thumbnail;
    Boolean status;
    Date createdAt;
    Date updatedAt;
}