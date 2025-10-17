package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.entity.CouponDetail;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {
    @NotNull(message = "Suất chiếu không được để trống")
    Integer showtimeId;

    @NotNull(message = "Danh sách ghế không được để trống")
    List<TicketItem> ticketItems = new ArrayList<>();

    List<ServiceItem> serviceItems = new ArrayList<>();

    String couponCode;

    private String paymentMethod;

    private Integer expireSeconds;

    private Discounts discounts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TicketItem {
        Integer seatId;
        Integer price;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ServiceItem {
        Integer additionalServiceId;
        Integer quantity;
        Integer price;
    }

    @Data
    public static class Discounts {
        private Integer totalDiscount;
        private List<CouponDetailRequest> coupons;
    }
}