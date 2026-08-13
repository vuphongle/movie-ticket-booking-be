package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.model.request.ChatRecommendationRequest;
import vn.edu.iuh.fit.model.response.ChatRecommendationResponse;
import vn.edu.iuh.fit.service.ChatRecommendationService;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatRecommendationService chatRecommendationService;

  @PostMapping("/recommendations")
  public ResponseEntity<ChatRecommendationResponse> getRecommendations(
      @Valid @RequestBody ChatRecommendationRequest request) {
    ChatRecommendationResponse response =
        chatRecommendationService.generateRecommendations(request);
    return ResponseEntity.ok(response);
  }
}
