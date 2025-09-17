package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponPreviewRequest {
    
    @NotEmpty(message = "Tickets không được để trống")
    List<TicketItem> tickets;
    
    List<ServiceItem> services;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TicketItem {
        Integer seatTypeId;
        Integer qty;
        BigDecimal unitPrice;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ServiceItem {
        Integer serviceId;
        Integer qty;
        BigDecimal unitPrice;
    }
}