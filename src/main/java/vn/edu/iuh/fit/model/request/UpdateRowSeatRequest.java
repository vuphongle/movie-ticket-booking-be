package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.SeatType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRowSeatRequest {
  @NotNull(message = "Auditorium ID không được để trống")
  Integer auditoriumId;

  @NotNull(message = "Row index không được để trống")
  Integer rowIndex; // 1, 2, 3, ...

  @NotNull(message = "Type không được để trống")
  SeatType type; // NORMAL, VIP

  @NotNull(message = "Status không được để trống")
  Boolean status; // true: available, false: not available
}
