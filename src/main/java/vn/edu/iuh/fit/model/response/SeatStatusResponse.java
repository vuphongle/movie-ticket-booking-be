package vn.edu.iuh.fit.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatStatusResponse {
  private Integer seatId;
  private Integer showtimeId;
  private SeatReservationStatus status; // BOOKED, HELD, hoặc CANCEL
}
