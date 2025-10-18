package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CancelMultipleSeatsRequest {
    @NotNull
    private Integer showtimeId;

    @NotNull
    private List<Integer> seatIds;
}