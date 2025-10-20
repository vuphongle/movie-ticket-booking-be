package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertReviewRequest {
  // User
  String userId;

  @NotNull(message = "Rating không được để trống")
  Integer rating;

  @NotEmpty(message = "Comment không được để trống")
  String comment;

  @NotNull(message = "Id phim không được để trống")
  Integer movieId;

  List<String> feeling;

  List<String> images;
}
