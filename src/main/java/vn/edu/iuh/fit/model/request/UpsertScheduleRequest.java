package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertScheduleRequest {
  @NotNull(message = "Movie id không được để trống")
  Integer movieId;

  @NotNull(message = "Start date không được để trống")
  Date startDate;

  @NotNull(message = "End date không được để trống")
  Date endDate;
}
