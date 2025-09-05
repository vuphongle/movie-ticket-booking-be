package vn.edu.iuh.fit.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.entity.Auditorium;
import vn.edu.iuh.fit.entity.Cinema;
import vn.edu.iuh.fit.entity.Showtime;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowtimeResponse {
    Cinema cinema;
    List<AuditoriumResponse> auditoriums;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AuditoriumResponse {
        Auditorium auditorium;
        List<Showtime> showtimes;
    }
}
