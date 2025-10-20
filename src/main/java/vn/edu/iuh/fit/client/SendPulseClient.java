package vn.edu.iuh.fit.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import vn.edu.iuh.fit.config.SendPulseConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendPulseClient {

  private final WebClient sendPulseWebClient;
  private final SendPulseConfig config;
  private final ObjectMapper objectMapper;

  private String accessToken;
  private long tokenExpiresAt = 0;

  /** Get OAuth access token from SendPulse */
  private String getAccessToken() {
    // Check if token is still valid
    if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
      return accessToken;
    }

    log.info("Requesting new SendPulse access token");

    try {
      ObjectNode requestBody = objectMapper.createObjectNode();
      requestBody.put("grant_type", "client_credentials");
      requestBody.put("client_id", config.getClientId());
      requestBody.put("client_secret", config.getClientSecret());

      JsonNode response =
          sendPulseWebClient
              .post()
              .uri("/oauth/access_token")
              .bodyValue(requestBody)
              .retrieve()
              .bodyToMono(JsonNode.class)
              .timeout(Duration.ofSeconds(10))
              .block();

      if (response != null && response.has("access_token")) {
        this.accessToken = response.get("access_token").asText();
        int expiresIn = response.get("expires_in").asInt(3600); // Default 1 hour
        this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 60) * 1000L; // 1 min buffer
        log.info("SendPulse access token obtained, expires in {} seconds", expiresIn);
        return this.accessToken;
      } else {
        throw new RuntimeException("Failed to get access token from SendPulse");
      }
    } catch (Exception e) {
      log.error("Error getting SendPulse access token: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to authenticate with SendPulse", e);
    }
  }

  /**
   * Send email via SendPulse REST API
   *
   * @param to Recipient email
   * @param subject Email subject
   * @param htmlBody HTML body content
   */
  public void sendEmail(String to, String subject, String htmlBody) {
    sendEmail(to, subject, htmlBody, null);
  }

  /**
   * Send email with attachment via SendPulse REST API
   *
   * @param to Recipient email
   * @param subject Email subject
   * @param htmlBody HTML body content
   * @param attachments Array of attachments (optional)
   */
  public void sendEmail(String to, String subject, String htmlBody, ObjectNode[] attachments) {
    try {
      String token = getAccessToken();

      ObjectNode emailData = objectMapper.createObjectNode();

      // From
      ObjectNode from = objectMapper.createObjectNode();
      from.put("name", config.getFromName());
      from.put("email", config.getFromEmail());
      emailData.set("from", from);

      // To
      ArrayNode toArray = objectMapper.createArrayNode();
      ObjectNode recipient = objectMapper.createObjectNode();
      recipient.put("email", to);
      toArray.add(recipient);
      emailData.set("to", toArray);

      // Subject and body
      emailData.put("subject", subject);
      emailData.put("html", htmlBody);

      // Attachments if present
      if (attachments != null && attachments.length > 0) {
        ArrayNode attachmentsArray = objectMapper.createArrayNode();
        for (ObjectNode attachment : attachments) {
          attachmentsArray.add(attachment);
        }
        emailData.set("attachments", attachmentsArray);
      }

      log.info("Sending email to {} via SendPulse API", to);

      JsonNode response =
          sendPulseWebClient
              .post()
              .uri("/smtp/emails")
              .header("Authorization", "Bearer " + token)
              .bodyValue(emailData)
              .retrieve()
              .bodyToMono(JsonNode.class)
              .timeout(Duration.ofSeconds(30))
              .onErrorResume(
                  error -> {
                    log.error("SendPulse API error: {}", error.getMessage());
                    return Mono.empty();
                  })
              .block();

      if (response != null) {
        log.info("SendPulse response: {}", response);
      } else {
        log.warn("No response from SendPulse API");
      }

    } catch (Exception e) {
      log.error("Error sending email via SendPulse: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to send email via SendPulse", e);
    }
  }

  /**
   * Create attachment object for SendPulse
   *
   * @param filename Filename
   * @param content Base64 encoded content or raw bytes
   * @param contentType MIME type
   * @return Attachment object
   */
  public ObjectNode createAttachment(String filename, byte[] content, String contentType) {
    ObjectNode attachment = objectMapper.createObjectNode();
    attachment.put("name", filename);
    attachment.put("content", Base64.getEncoder().encodeToString(content));
    attachment.put("type", contentType);
    return attachment;
  }
}
