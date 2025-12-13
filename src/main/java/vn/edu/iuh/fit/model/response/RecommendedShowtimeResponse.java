package vn.edu.iuh.fit.model.response;

import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class RecommendedShowtimeResponse {
  Integer id;
  LocalDate date;
  String startTime;
  String endTime;
  String graphicsType;
  String translationType;
  Integer cinemaId;
  String cinemaName;
  String cinemaAddress;
  Integer auditoriumId;
  String auditoriumName;
  String auditoriumType;
}
