package vn.edu.iuh.fit.model.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieWithShowtimesDto {
  private Integer id;
  private String name;
  private String nameEn;
  private String description;
  private Integer duration;
  private String poster;
  private Double rating;
  private Integer releaseYear;
  private String age;
  private String trailer;
  private Boolean status;
  private String slug;
  private Date createdAt;
  private Date updatedAt;
  private String graphics; // JSON string
  private String translations; // JSON string
  private Integer countryId;

  private List<ShowtimeDto> showtimes = new ArrayList<>();
}
