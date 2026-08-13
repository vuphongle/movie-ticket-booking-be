package vn.edu.iuh.fit.model.request;

import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.NotBlank;
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
public class ChatRecommendationRequest {

  @NotBlank(message = "Nội dung câu hỏi không được để trống")
  String message;

  String language;

  String conversationId;
}
