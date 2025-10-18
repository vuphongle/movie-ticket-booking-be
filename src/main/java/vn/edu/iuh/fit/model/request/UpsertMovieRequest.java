package vn.edu.iuh.fit.model.request;

import java.util.Date;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.GraphicsType;
import vn.edu.iuh.fit.model.enums.MovieAge;
import vn.edu.iuh.fit.model.enums.TranslationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertMovieRequest {
  String name;
  String nameEn;
  String trailer;
  String description;
  String poster;
  Integer releaseYear;
  Integer duration;
  Boolean status;
  Date showDate;
  MovieAge age;
  Integer countryId;
  List<Integer> genreIds;
  List<Integer> directorIds;
  List<Integer> actorIds;
  List<GraphicsType> graphics;
  List<TranslationType> translations;
}
