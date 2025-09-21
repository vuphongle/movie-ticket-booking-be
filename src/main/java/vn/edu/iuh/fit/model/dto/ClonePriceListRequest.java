package vn.edu.iuh.fit.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClonePriceListRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name;
    
    private Integer priority;
    
    private Boolean status = false; // Default to inactive
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date validFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date validTo;
}