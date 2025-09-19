package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertProductRequest {
    @NotBlank(message = "SKU is required")
    String sku;
    
    @NotBlank(message = "Product name is required")
    String name;
    
    String description;
    
    String unit;
    
    String thumbnail;
    
    @NotNull(message = "Status is required")
    Boolean status;
}