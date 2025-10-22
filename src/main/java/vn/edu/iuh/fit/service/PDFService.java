package vn.edu.iuh.fit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.entity.OrderServiceItem;
import vn.edu.iuh.fit.entity.OrderTicketItem;
import vn.edu.iuh.fit.model.request.CouponDetailRequest;
import vn.edu.iuh.fit.model.request.CreateOrderRequest;

@Service
@RequiredArgsConstructor
public class PDFService {

  private final S3Service s3Service;

  public String generateOrderPdfToS3(Order order, String qrCodeBase64) throws Exception {
    // Trích xuất coupon
    Map<String, Object> couponData = new HashMap<>();
    if (order.getRequestSnapshot() != null) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        CreateOrderRequest request =
            objectMapper.readValue(order.getRequestSnapshot(), CreateOrderRequest.class);

        if (request.getDiscounts() != null && request.getDiscounts().getCoupons() != null) {
          couponData.put("coupons", request.getDiscounts().getCoupons());
        }
      } catch (Exception e) {
        // log nếu cần
      }
    }

    // 1. Build HTML
    String html = buildHtml(order, qrCodeBase64, couponData);

    // 2. Render PDF vào byte[]
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PdfRendererBuilder builder = new PdfRendererBuilder();

    builder.useFont(new File("fonts/NotoSans-Regular.ttf"), "Noto Sans");
    builder.useFont(new File("fonts/NotoSans-Bold.ttf"), "Noto Sans");

    builder.withHtmlContent(html, null);
    builder.toStream(os);
    builder.run();

    byte[] pdfBytes = os.toByteArray();

    // 3. Upload PDF lên S3
    String fileName = "orders/order-" + order.getId() + ".pdf";
    return s3Service.uploadImage(fileName, pdfBytes, "application/pdf");
  }

  private String buildHtml(Order order, String qrCodeBase64, Map<String, Object> couponData) {
    String qrImg = qrCodeBase64 != null ? "data:image/png;base64," + qrCodeBase64 : "";

    String userName = order.getUser().getName();
    String userPhone = order.getUser().getPhone();
    String movieTitle = order.getShowtime().getMovie().getName();
    String cinemaName = order.getShowtime().getAuditorium().getCinema().getName();
    String auditoriumName = order.getShowtime().getAuditorium().getName();
    String showDate = order.getShowtime().getDate().toString();
    String startTime = order.getShowtime().getStartTime().toString();
    String endTime = order.getShowtime().getEndTime().toString();
    String status = order.getStatus().name();

    int totalTicketPrice =
        order.getTicketItems().stream().mapToInt(OrderTicketItem::getPrice).sum();

    int totalServicePrice =
        order.getServiceItems().stream().mapToInt(s -> s.getPrice() * s.getQuantity()).sum();

    int totalPrice =
        totalTicketPrice
            + totalServicePrice
            - (order.getDiscount() != null ? order.getDiscount() : 0);

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html>")
        .append(
            "<html xmlns='http://www.w3.org/1999/xhtml'><head><meta charset='UTF-8'/><title>Order PDF</title>")
        .append("<style>")
        .append(
            "@font-face {font-family:'Noto Sans'; src: url('fonts/NotoSans-Regular.ttf') format('truetype'); font-weight: normal;}")
        .append(
            "@font-face {font-family:'Noto Sans'; src: url('fonts/NotoSans-Bold.ttf') format('truetype'); font-weight: bold;}")
        .append("@page { size: 90mm 220mm; margin: 5mm; }")
        .append(
            "body { font-family:'Noto Sans', sans-serif; font-size:10pt; margin:0; padding:0; }")
        .append(".container { width:100%; padding:0; }")
        .append(
            ".header,h2{text-align:center; margin:0;} .content h3{text-align:center; margin:5px 0;}")
        .append(".qr-code img { width:180px; height:180px; display:block; margin:10px auto; }")
        .append("table { width:100%; border-collapse: collapse; margin:5px 0; font-size:9pt; }")
        .append("th, td { border:1px solid #000; padding:3px 5px; text-align:left; }")
        .append("th { background-color:#f2f2f2; font-weight:bold; }")
        .append(".footer { text-align:center; font-size:8pt; margin-top:5px; color:#888; }")
        .append("</style></head><body>")
        .append("<div class='container'>")
        .append("<div class='header'><h2>GO CINEMA</h2></div>")
        .append("<div class='content'><h3>Vé điện tử của bạn</h3>");

    if (!qrImg.isEmpty()) {
      sb.append("<div class='qr-code'><img src='").append(qrImg).append("' /></div>");
    }

    sb.append("<p>Xin chào <strong>")
        .append(StringEscapeUtils.escapeXml10(userName))
        .append(" (")
        .append(StringEscapeUtils.escapeXml10(userPhone))
        .append(")</strong>,</p>")
        .append("<p>Thông tin đơn hàng của bạn:</p>")
        .append("<ul>")
        .append("<li>Mã đơn hàng: <strong>")
        .append(order.getId())
        .append("</strong></li>")
        .append("<li>Phim: <strong>")
        .append(StringEscapeUtils.escapeXml10(movieTitle))
        .append("</strong></li>")
        .append("<li>Ngày/giờ chiếu: <strong>")
        .append(showDate)
        .append(" | ")
        .append(startTime)
        .append(" - ")
        .append(endTime)
        .append("</strong></li>")
        .append("<li>Rạp / Phòng: <strong>")
        .append(StringEscapeUtils.escapeXml10(cinemaName))
        .append(" / ")
        .append(StringEscapeUtils.escapeXml10(auditoriumName))
        .append("</strong></li>")
        .append("<li>Trạng thái: <strong>")
        .append(
            status.equals("CONFIRMED")
                ? "Đã xác nhận"
                : (status.equals("CANCELLED") ? "Đã hủy" : status))
        .append("</strong></li></ul>");

    // Danh sách ghế
    sb.append(
        "<h4>Danh sách ghế:</h4><table><thead><tr><th>Ghế</th><th>Loại</th><th>Giá (VNĐ)</th></tr></thead><tbody>");
    for (OrderTicketItem ticket : order.getTicketItems()) {
      sb.append("<tr><td>")
          .append(StringEscapeUtils.escapeXml10(ticket.getSeat().getCode()))
          .append("</td>")
          .append("<td>")
          .append(StringEscapeUtils.escapeXml10(String.valueOf(ticket.getSeat().getType())))
          .append("</td>")
          .append("<td>")
          .append(String.format("%,d", ticket.getPrice()))
          .append("</td></tr>");
    }
    sb.append("</tbody></table>");

    // Danh sách dịch vụ
    List<OrderServiceItem> services = order.getServiceItems();
    if (services != null && !services.isEmpty()) {
      sb.append(
          "<h4>Combo/Dịch vụ:</h4><table><thead><tr><th>Sản phẩm</th><th>Số lượng</th><th>Giá (VNĐ)</th></tr></thead><tbody>");
      for (OrderServiceItem s : services) {
        sb.append("<tr><td>")
            .append(StringEscapeUtils.escapeXml10(s.getAdditionalService().getName()))
            .append("</td>")
            .append("<td>")
            .append(s.getQuantity())
            .append("</td>")
            .append("<td>")
            .append(String.format("%,d", s.getPrice() * s.getQuantity()))
            .append("</td></tr>");
      }
      sb.append("</tbody></table>");
    } else {
      sb.append("<p>Không có combo/dịch vụ đi kèm.</p>");
    }

    // Thông tin coupon
    if (couponData.containsKey("coupons")) {
      sb.append("<h4>Mã giảm giá / Quà tặng:</h4>");
      sb.append(
          "<table border='1' cellspacing='0' cellpadding='4' style='border-collapse:collapse;width:100%;'><thead><tr><th>Mã</th><th>Loại</th><th>Giảm</th><th>Quà tặng</th></tr></thead><tbody>");

      List<CouponDetailRequest> coupons = (List<CouponDetailRequest>) couponData.get("coupons");
      boolean hasValidCoupon = false;

      for (CouponDetailRequest c : coupons) {
        String code = c.getCode();
        String displayCode = "";

        if (code != null && !code.contains("null")) {
          int firstUnderscore = code.indexOf("_", "COUPON_".length());
          int lastUnderscore = code.lastIndexOf("_");
          if (firstUnderscore != -1 && lastUnderscore != -1 && firstUnderscore < lastUnderscore) {
            displayCode = code.substring(firstUnderscore + 1, lastUnderscore);
          } else {
            displayCode = code;
          }
        }

        hasValidCoupon = true;
        sb.append("<tr>")
            .append("<td style='word-break:break-all;'>")
            .append(StringEscapeUtils.escapeXml10(displayCode))
            .append("</td>")
            .append("<td>")
            .append(StringEscapeUtils.escapeXml10(c.getType()))
            .append("</td>")
            .append("<td>")
            .append(StringEscapeUtils.escapeXml10(String.valueOf(c.getDiscount())))
            .append("</td>")
            .append("<td>");

        if (c.getGifts() != null && !c.getGifts().isEmpty()) {
          for (CouponDetailRequest.GiftItem gift : c.getGifts()) {
            sb.append(StringEscapeUtils.escapeXml10(gift.getServiceName())).append("<br/>");
          }
        }
        sb.append("</td></tr>");
      }

      if (!hasValidCoupon) {
        sb.append(
            "<tr><td colspan='4' style='text-align:center;'>Không áp dụng mã khuyến mãi nào</td></tr>");
      }

      sb.append("</tbody></table>");
    }

    sb.append("<h4>Tổng giá:</h4>")
        .append("<p>Giảm giá: <strong>")
        .append(String.format("%,d", order.getDiscount() != null ? order.getDiscount() : 0))
        .append(" VNĐ</strong></p>")
        .append("<p>Tổng thanh toán: <strong>")
        .append(String.format("%,d", totalPrice))
        .append(" VNĐ</strong></p>")
        .append("</div>") // đóng content
        .append("<div class='footer'>2025 Go Cinema. Mọi quyền được bảo lưu.</div>")
        .append("</div></body></html>");

    return sb.toString();
  }
}
