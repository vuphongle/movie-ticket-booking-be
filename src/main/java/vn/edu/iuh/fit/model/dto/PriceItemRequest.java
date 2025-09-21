package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceItemRequest {
    
    @NotNull(message = "Price list ID is required")
    Integer priceListId;
    
    @NotNull(message = "Target type is required") 
    TargetType targetType;
    
    Integer targetId; // nullable when targetType = TICKET
    
    // Ticket-specific fields (when targetType = TICKET)
    SeatType seatType; // nullable, null = wildcard
    GraphicsType graphicsType; // nullable, null = wildcard
    ScreeningTimeType screeningTimeType; // nullable, null = wildcard
    DayType dayType; // nullable, null = wildcard
    AuditoriumType auditoriumType; // nullable, null = wildcard
    
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    Integer price;
    
    @Min(value = 1, message = "Minimum quantity must be positive")
    Integer minQty; // nullable
    
    @NotNull(message = "Priority is required")
    Integer priority;
    
    @Builder.Default
    Boolean status = true; // Default to active
}