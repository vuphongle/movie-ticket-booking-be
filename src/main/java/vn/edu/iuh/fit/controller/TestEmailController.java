package vn.edu.iuh.fit.controller;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.edu.iuh.fit.client.SendPulseClient;

/**
 * Test controller để debug SendPulse email Endpoint này chỉ nên được sử dụng trong môi trường
 * development
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestEmailController {

  private final SendPulseClient sendPulseClient;
  private final TemplateEngine templateEngine;

  @Value("${app.frontend.host}")
  private String frontendHost;

  @Value("${app.frontend.port}")
  private String frontendPort;

  private String getFrontendDomain() {
    if (frontendPort == null || frontendPort.trim().isEmpty()) {
      return frontendHost;
    }
    return "%s:%s".formatted(frontendHost, frontendPort);
  }

  /** Test gửi email đơn giản GET /api/v1/test/send-email?to=recipient@example.com */
  @GetMapping("/send-email")
  public ResponseEntity<Map<String, Object>> testSendEmail(
      @RequestParam(required = false, defaultValue = "ple370235@gmail.com") String to) {

    Map<String, Object> response = new HashMap<>();

    try {
      log.info("Testing SendPulse email to: {}", to);

      String subject = "Test Email từ Go Cinema";
      String htmlBody =
          """
          <html>
          <body style="font-family: Arial, sans-serif;">
            <h2 style="color: #e50914;">🎬 Go Cinema Test Email</h2>
            <p>Đây là email test từ hệ thống Go Cinema.</p>
            <p>Nếu bạn nhận được email này, SendPulse integration đã hoạt động!</p>
            <hr>
            <p style="color: #666; font-size: 12px;">
              Email được gửi lúc: """
              + java.time.LocalDateTime.now()
              + """
            </p>
          </body>
          </html>
          """;

      sendPulseClient.sendEmail(to, subject, htmlBody);

      response.put("success", true);
      response.put("message", "Email sent successfully!");
      response.put("recipient", to);
      response.put("timestamp", java.time.LocalDateTime.now());

      log.info("Test email sent successfully to: {}", to);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Failed to send test email: {}", e.getMessage(), e);

      response.put("success", false);
      response.put("message", "Failed to send email: " + e.getMessage());
      response.put("error", e.getClass().getSimpleName());

      return ResponseEntity.internalServerError().body(response);
    }
  }

  /**
   * Test template rendering và gửi email reset password GET
   * /api/v1/test/send-reset-password?to=recipient@example.com&username=TestUser
   */
  @GetMapping("/send-reset-password")
  public ResponseEntity<Map<String, Object>> testResetPasswordEmail(
      @RequestParam(required = false, defaultValue = "ple370235@gmail.com") String to,
      @RequestParam(required = false, defaultValue = "Test User") String username) {

    Map<String, Object> response = new HashMap<>();

    try {
      log.info("Testing reset password email with template to: {}", to);

      // Create context for template
      Context context = new Context();
      context.setVariable("username", username);
      context.setVariable("token", "test-token-12345");
      context.setVariable("frontendDomain", getFrontendDomain());

      // Process template
      String htmlContent = templateEngine.process("mail-template/reset-password", context);

      log.debug("Template rendered, HTML length: {} characters", htmlContent.length());

      if (htmlContent == null || htmlContent.trim().isEmpty()) {
        throw new RuntimeException("Template rendering returned empty content");
      }

      // Send email
      sendPulseClient.sendEmail(to, "Test - Đặt lại mật khẩu Go Cinema", htmlContent);

      response.put("success", true);
      response.put("message", "Reset password email sent successfully!");
      response.put("recipient", to);
      response.put("htmlLength", htmlContent.length());
      response.put("timestamp", java.time.LocalDateTime.now());

      log.info("Reset password test email sent successfully to: {}", to);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Failed to send reset password test email: {}", e.getMessage(), e);

      response.put("success", false);
      response.put("message", "Failed to send email: " + e.getMessage());
      response.put("error", e.getClass().getSimpleName());

      return ResponseEntity.internalServerError().body(response);
    }
  }

  /**
   * Test template rendering only (không gửi email) GET
   * /api/v1/test/render-template?template=reset-password
   */
  @GetMapping("/render-template")
  public ResponseEntity<Map<String, Object>> testTemplateRendering(
      @RequestParam(required = false, defaultValue = "reset-password") String template) {

    Map<String, Object> response = new HashMap<>();

    try {
      log.info("Testing template rendering for: {}", template);

      // Create context
      Context context = new Context();
      context.setVariable("username", "Test User");
      context.setVariable("token", "test-token-12345");
      context.setVariable("frontendDomain", getFrontendDomain());

      // Process template
      String htmlContent = templateEngine.process("mail-template/" + template, context);

      response.put("success", true);
      response.put("template", template);
      response.put("htmlLength", htmlContent.length());
      response.put("isEmpty", htmlContent == null || htmlContent.trim().isEmpty());
      response.put(
          "htmlPreview", htmlContent.substring(0, Math.min(200, htmlContent.length())) + "...");

      log.info("Template rendered successfully, length: {} characters", htmlContent.length());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Failed to render template: {}", e.getMessage(), e);

      response.put("success", false);
      response.put("message", "Failed to render template: " + e.getMessage());
      response.put("error", e.getClass().getSimpleName());

      return ResponseEntity.internalServerError().body(response);
    }
  }

  /** Kiểm tra cấu hình SendPulse GET /api/v1/test/sendpulse-config */
  @GetMapping("/sendpulse-config")
  public ResponseEntity<Map<String, Object>> checkConfig() {
    Map<String, Object> response = new HashMap<>();

    try {
      // Không log sensitive data như secret
      response.put("configured", true);
      response.put(
          "message", "SendPulse client is configured. Check logs for details when sending email.");
      response.put(
          "endpoints",
          Map.of(
              "simple", "/api/v1/test/send-email?to=your@email.com",
              "resetPassword",
                  "/api/v1/test/send-reset-password?to=your@email.com&username=YourName",
              "renderOnly", "/api/v1/test/render-template?template=reset-password"));

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      response.put("configured", false);
      response.put("error", e.getMessage());
      return ResponseEntity.internalServerError().body(response);
    }
  }
}
