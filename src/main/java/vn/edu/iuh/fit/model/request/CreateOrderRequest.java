package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

  private String platform;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class TicketItem {
    Integer seatId;
    Integer price;
    Integer priceId;
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
    Integer priceId;
  }

  @Data
  public static class Discounts {
    private Integer totalDiscount;
    private List<CouponDetailRequest> coupons;
  }
}
