package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelMultipleSeatsRequest {
  @NotNull private Integer showtimeId;

  @NotNull private List<Integer> seatIds;
}
