package vn.edu.iuh.fit.model.response;

import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.MovieAge;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class ShowtimeDetailResponse {
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
  Integer auditoriumTotalSeats;
  Integer auditoriumTotalRows;
  Integer auditoriumTotalColumns;
  String auditoriumType;
  Integer movieId;
  String movieName;
  String movieSlug;
  String moviePoster;
  MovieAge movieAge;
  Double movieRating;
  Integer movieDuration;
}
