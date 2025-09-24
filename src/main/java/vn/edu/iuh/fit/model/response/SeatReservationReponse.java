package vn.edu.iuh.fit.model.response;

import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatReservationReponse {
    Integer seatId;
    Integer showtimeId;
    SeatReservationStatus status;
}