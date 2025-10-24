package vn.edu.iuh.fit.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CinemaMovieRevenueDto {
  Integer cinemaId;
  String cinemaCode;
  String cinemaName;
  Integer movieId;
  String movieCode;
  String movieName;
  Integer totalTickets;
  Integer ticketRevenue;
  Integer serviceRevenue;
  Integer totalDiscount;
  Integer revenueBeforeDiscount;
  Integer totalRevenue;
}
