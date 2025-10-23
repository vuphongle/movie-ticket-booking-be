package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CinemaRevenueDto {
  Integer cinemaId;
  String cinemaCode; // Mã rạp
  String cinemaName;
  Integer totalTickets;
  Integer ticketRevenue; // Doanh thu vé
  Integer serviceRevenue; // Doanh thu dịch vụ
  Integer totalDiscount; // Tổng chiết khấu
  Integer revenueBeforeDiscount; // Doanh thu trước CK
  Integer totalRevenue; // Doanh thu sau CK

  // Constructor cũ để tương thích ngược
  public CinemaRevenueDto(
      Integer cinemaId, String cinemaName, Integer totalTickets, Integer totalRevenue) {
    this.cinemaId = cinemaId;
    this.cinemaCode = "R" + String.format("%04d", cinemaId);
    this.cinemaName = cinemaName;
    this.totalTickets = totalTickets;
    this.ticketRevenue = 0;
    this.serviceRevenue = 0;
    this.totalDiscount = 0;
    this.revenueBeforeDiscount = totalRevenue;
    this.totalRevenue = totalRevenue;
  }
}
