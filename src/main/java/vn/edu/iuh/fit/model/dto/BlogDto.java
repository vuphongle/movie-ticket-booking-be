package vn.edu.iuh.fit.model.dto;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogDto {
  Integer id;
  String title;
  String slug;
  String description;
  String thumbnail;
  LocalDateTime publishedAt;
}
