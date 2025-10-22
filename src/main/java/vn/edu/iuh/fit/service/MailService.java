package vn.edu.iuh.fit.service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.edu.iuh.fit.client.SendPulseClient;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.model.enums.GraphicsType;
import vn.edu.iuh.fit.model.enums.TranslationType;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
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

  // Send mail confirm registration
  @Async
  public void sendMailConfirmRegistration(Map<String, String> data) {
    log.info("Sending registration confirmation email to {}", data.get("email"));
    try {
      // Create the Thymeleaf context
      Context context = new Context();
      context.setVariable("username", data.get("username"));
      context.setVariable("token", data.get("token"));
      context.setVariable("frontendDomain", getFrontendDomain());

      // Use the template engine to process the template
      String htmlContent = templateEngine.process("mail-template/confirmation-account", context);

      if (htmlContent == null || htmlContent.trim().isEmpty()) {
        log.error("Template processing returned empty content!");
        throw new RuntimeException("Email template processing failed - empty content");
      }

      // Send via SendPulse REST API
      sendPulseClient.sendEmail(data.get("email"), "Xác nhận đăng ký tài khoản", htmlContent);

      log.info("Registration confirmation email sent successfully to {}", data.get("email"));
    } catch (Exception e) {
      log.error("Error when sending registration email: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to send registration email: " + e.getMessage());
    }
  }

  // Send mail reset password
  @Async
  public void sendMailResetPassword(Map<String, String> data) {
    log.info("Sending password reset email to {}", data.get("email"));
    try {
      // Create the Thymeleaf context
      Context context = new Context();
      context.setVariable("username", data.get("username"));
      context.setVariable("token", data.get("token"));
      context.setVariable("frontendDomain", getFrontendDomain());

      // Use the template engine to process the template
      String htmlContent = templateEngine.process("mail-template/reset-password", context);

      if (htmlContent == null || htmlContent.trim().isEmpty()) {
        log.error("Template processing returned empty content!");
        throw new RuntimeException("Email template processing failed - empty content");
      }

      // Send via SendPulse REST API
      sendPulseClient.sendEmail(data.get("email"), "Xác nhận đặt lại mật khẩu", htmlContent);

      log.info("Password reset email sent successfully to {}", data.get("email"));
    } catch (Exception e) {
      log.error("Error sending password reset email: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
    }
  }

    @Async
    public void sendMailConfirmOrder(Map<String, Object> data, byte[] qrCodeImage) {
        try {

            User user = (User) data.get("user");
            Order order = (Order) data.get("order");

            // --- Format giá tiền bằng DecimalFormat ---
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(',');
            symbols.setDecimalSeparator(',');
            DecimalFormat df = new DecimalFormat("#,###", symbols);

            GraphicsType graphicsTypeEnum = order.getShowtime().getGraphicsType();
            String formattedGraphicsType = switch (graphicsTypeEnum) {
                case _2D -> "2D";
                case _3D -> "3D";
            };

            TranslationType translationTypeEnum = order.getShowtime().getTranslationType(); // enum
            String formattedTranslationType = switch (translationTypeEnum) {
                case DUBBING -> "Lồng tiếng";
                case SUBTITLING -> "Phụ đề";
            };

            Context context = new Context();
            context.setVariable("userName", user.getName());
            context.setVariable("userPhone", user.getPhone());
            context.setVariable("orderId", order.getId());
            context.setVariable("movieTitle", order.getShowtime().getMovie().getName());
            context.setVariable("graphicsType", formattedGraphicsType);
            context.setVariable("translationType", formattedTranslationType);
            context.setVariable("showDate", order.getShowtime().getDate().toString());
            context.setVariable("startTime", order.getShowtime().getStartTime().toString());
            context.setVariable("endTime", order.getShowtime().getEndTime().toString());
            context.setVariable("cinemaName", order.getShowtime().getAuditorium().getCinema().getName());
            context.setVariable("auditoriumName", order.getShowtime().getAuditorium().getName());
            context.setVariable("status", order.getStatus().name());
            context.setVariable("totalPrice", df.format(order.getTotalPrice()) + " VNĐ");
            context.setVariable("discountPrice", df.format(order.getDiscountPrice()) + " VNĐ");
            context.setVariable("ticketItems", order.getTicketItems()); // danh sách ghế
            context.setVariable("serviceItems", order.getServiceItems());
            context.setVariable("coupons", data.get("coupons"));

            context.setVariable("qrCodeUrl", order.getQrCodePath());
            String htmlContent = templateEngine.process("mail-template/order-confirm", context);


            // Send via SendPulse REST API
            sendPulseClient.sendEmail(user.getEmail(), "Vé điện tử Go Cinema", htmlContent);

            log.info("Sent order confirmation email to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error sending order confirmation email: {}", e.getMessage());
        }
    }
}
