package vn.edu.iuh.fit.model.response;

import java.util.*;
import lombok.*;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.model.enums.GraphicsType;
import vn.edu.iuh.fit.model.enums.MovieAge;
import vn.edu.iuh.fit.model.enums.TranslationType;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MovieResponse {
  Integer id;
  String name;
  String nameEn;
  String slug;
  String trailer;
  String description;
  String poster;
  Integer releaseYear;
  Double rating;
  Integer duration;
  Boolean status;
  Date showDate;
  Date createdAt;
  Date updatedAt;
  Date publishedAt;
  List<GraphicsType> graphics;
  List<TranslationType> translations;
  MovieAge age;
  Country country;
  Set<Genre> genres;
  Set<Director> directors;
  Set<Actor> actors;
  Set<Review> reviews;
}
