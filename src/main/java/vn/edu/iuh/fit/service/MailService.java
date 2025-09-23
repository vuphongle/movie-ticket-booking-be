package vn.edu.iuh.fit.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.entity.User;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend.host}")
    private String frontendHost;

    @Value("${app.frontend.port}")
    private String frontendPort;

    // Send mail confirm registration
    @Async
    public void sendMailConfirmRegistration(Map<String, String> data) {
        log.info("sendMailConfirmRegistration");
        log.info("Sending email request : {}", data);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(data.get("email"));
            helper.setSubject("Xác nhận đăng ký tài khoản");

            // Create the Thymeleaf context
            Context context = new Context();
            context.setVariable("username", data.get("username"));
            context.setVariable("token", data.get("token"));
            context.setVariable("frontendDomain", "%s:%s".formatted(frontendHost, frontendPort));

            // Use the template engine to process the template
            String htmlContent = templateEngine.process("mail-template/confirmation-account", context);
            helper.setText(htmlContent, true); // Enable HTML content

            javaMailSender.send(message);
        } catch (MessagingException e) {
            log.error("Error when sending email: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // Send mail reset password
    @Async
    public void sendMailResetPassword(Map<String, String> data) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(data.get("email"));
            helper.setSubject("Xác nhận đặt lại mật khẩu");

            // Create the Thymeleaf context
            Context context = new Context();
            context.setVariable("username", data.get("username"));
            context.setVariable("token", data.get("token"));
            context.setVariable("frontendDomain", "%s:%s".formatted(frontendHost, frontendPort));

            // Use the template engine to process the template
            String htmlContent = templateEngine.process("mail-template/reset-password", context);
            helper.setText(htmlContent, true); // Enable HTML content

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Async
    public void sendMailConfirmOrder(Map<String, Object> data, byte[] qrCodeImage) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            User user = (User) data.get("user");
            Order order = (Order) data.get("order");

            helper.setTo(user.getEmail());
            helper.setSubject("Vé điện tử Go Cinema");

            Context context = new Context();
            context.setVariable("userName", user.getName());
            context.setVariable("userPhone", user.getPhone());
            context.setVariable("orderId", order.getId());
            context.setVariable("movieTitle", order.getShowtime().getMovie().getName());
            context.setVariable("graphicsType", order.getShowtime().getGraphicsType()); // 2D/3D
            context.setVariable("translationType", order.getShowtime().getTranslationType()); // SUB/DUB
            context.setVariable("showDate", order.getShowtime().getDate().toString());
            context.setVariable("startTime", order.getShowtime().getStartTime().toString());
            context.setVariable("endTime", order.getShowtime().getEndTime().toString());
            context.setVariable("cinemaName", order.getShowtime().getAuditorium().getCinema().getName());
            context.setVariable("auditoriumName", order.getShowtime().getAuditorium().getName());
            context.setVariable("status", order.getStatus().name());
            context.setVariable("totalPrice", order.getTotalPrice());
            context.setVariable("discountPrice", order.getDiscountPrice());
            context.setVariable("ticketItems", order.getTicketItems()); // danh sách ghế
            context.setVariable("serviceItems", order.getServiceItems());
            context.setVariable("qrCodePath", "ticketQr");

            String htmlContent = templateEngine.process("mail-template/order-confirm", context);
            helper.setText(htmlContent, true);

            // Thêm QR code inline
            helper.addInline("ticketQr", new ByteArrayResource(qrCodeImage), "image/png");

            javaMailSender.send(message);
            log.info("Sent order confirmation email to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error sending order confirmation email: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

}
