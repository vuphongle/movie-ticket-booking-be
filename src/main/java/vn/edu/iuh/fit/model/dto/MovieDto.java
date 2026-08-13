package vn.edu.iuh.fit.model.dto;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.MovieAge;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieDto {
  Integer id;
  String name;
  String slug;
  String description;
  String poster;
  String trailer;
  MovieAge age;
  Double rating;
  List<String> genres;
}
