package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieRevenueDto {
  Integer movieId;
  String movieCode;
  String movieName;
  Integer totalTickets;
  Integer totalDiscount;
  Integer revenueBeforeDiscount;
  Integer totalRevenue;

  // Constructor cũ để tương thích ngược
  public MovieRevenueDto(
      Integer movieId, String movieName, Integer totalTickets, Integer totalRevenue) {
    this.movieId = movieId;
    this.movieCode = "P" + String.format("%04d", movieId);
    this.movieName = movieName;
    this.totalTickets = totalTickets;
    this.totalDiscount = 0;
    this.revenueBeforeDiscount = totalRevenue;
    this.totalRevenue = totalRevenue;
  }
}
