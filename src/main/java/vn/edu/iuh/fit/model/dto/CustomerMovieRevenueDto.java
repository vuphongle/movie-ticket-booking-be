package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerMovieRevenueDto {
  Integer customerId;
  String customerCode;
  String customerName;
  String customerEmail;
  Integer movieId;
  String movieCode;
  String movieName;
  Integer totalOrders;
  Integer totalTickets;
  Integer ticketRevenue;
  Integer serviceRevenue;
  Integer totalDiscount;
  Integer revenueBeforeDiscount;
  Integer totalRevenue;
}
