package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import vn.edu.iuh.fit.model.enums.SeatType;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatResponse {
  Integer id;
  Integer rowIndex;
  Integer colIndex;
  String code;
  SeatType type;
  Boolean status;
  SeatReservationStatus reservationStatus;
  Integer priceId;
  Integer price;
}
