package vn.edu.iuh.fit.model.response;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
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
public class RecommendedMovieResponse {
  Integer movieId;
  String name;
  String slug;
  String poster;
  MovieAge ageRating;
  Double rating;
  List<String> genres;
  List<String> genreDisplayNames;
  List<String> reasons;
  List<String> showtimes;
}
