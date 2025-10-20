package vn.edu.iuh.fit.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.entity.OrderServiceItem;
import vn.edu.iuh.fit.entity.OrderTicketItem;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PDFService {

    private static final String STORAGE_DIR = "pdf-orders";

    public String generateOrderPdf(Order order, String qrCodeBase64) throws Exception {
        Path path = Paths.get(STORAGE_DIR);
        if (!Files.exists(path)) Files.createDirectories(path);

        String fileName = "order-" + order.getId() + ".pdf";
        Path filePath = path.resolve(fileName);

        String html = buildHtml(order, qrCodeBase64);

        try (FileOutputStream os = new FileOutputStream(filePath.toFile())) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            // Thêm font
            builder.useFont(new File("fonts/Roboto-Regular.ttf"), "Roboto");
            builder.useFont(new File("fonts/Roboto-Bold.ttf"), "Roboto");
            builder.run();
        }

        return filePath.toString();
    }

    private String buildHtml(Order order, String qrCodeBase64) {
        String qrImg = qrCodeBase64 != null ? "data:image/png;base64," + qrCodeBase64 : "";

        String userName = StringEscapeUtils.escapeXml10(order.getUser().getName());
        String userPhone = StringEscapeUtils.escapeXml10(order.getUser().getPhone());
        String movieTitle = StringEscapeUtils.escapeXml10(order.getShowtime().getMovie().getName());
        String cinemaName = StringEscapeUtils.escapeXml10(order.getShowtime().getAuditorium().getCinema().getName());
        String auditoriumName = StringEscapeUtils.escapeXml10(order.getShowtime().getAuditorium().getName());
        String showDate = order.getShowtime().getDate().toString();
        String startTime = order.getShowtime().getStartTime().toString();
        String endTime = order.getShowtime().getEndTime().toString();
        String status = order.getStatus().name();

        // Tính tổng giá vé
        int totalTicketPrice = order.getTicketItems().stream()
                .mapToInt(OrderTicketItem::getPrice)
                .sum();

        // Tổng giá combo/dịch vụ
        int totalServicePrice = order.getServiceItems().stream()
                .mapToInt(s -> s.getPrice() * s.getQuantity())
                .sum();

        int totalPrice = totalTicketPrice + totalServicePrice - (order.getDiscount() != null ? order.getDiscount() : 0);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">")
                .append("<head>")
                .append("<meta charset=\"UTF-8\"/>")
                .append("<title>Order PDF</title>")
                .append("<style>")
                .append(".container{width:80%;margin:0 auto;font-family:Arial,sans-serif;} ")
                .append(".header,h2{text-align:center;} .qr-code{text-align:center;margin:20px 0;} ")
                .append("table{width:100%;border-collapse:collapse;margin:10px 0;} ")
                .append("th,td{border:1px solid #ccc;padding:5px;text-align:left;} ")
                .append(".footer{text-align:center;font-size:12px;margin-top:20px;color:#888;}")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class='container'>")
                .append("<div class='header'><h2>GO CINEMA</h2></div>")
                .append("<div class='content'>")
                .append("<h3>Vé điện tử của bạn</h3>");
        if (!qrImg.isEmpty()) {
            sb.append("<div class='qr-code'><img src='").append(qrImg).append("' width='200' height='200'/></div>");
        }
        sb.append("<p>Vui lòng giữ QR code này để xuất trình khi đến rạp.</p>")
                .append("</div>")
                .append("<p>Xin chào <strong>").append(userName).append(" (").append(userPhone).append(")</strong>,</p>")
                .append("<p>Thông tin đơn hàng của bạn:</p>")
                .append("<ul>")
                .append("<li>Mã đơn hàng: <strong>").append(order.getId()).append("</strong></li>")
                .append("<li>Phim: <strong>").append(movieTitle).append("</strong></li>")
                .append("<li>Ngày/giờ chiếu: <strong>").append(showDate).append(" | ").append(startTime).append(" - ").append(endTime).append("</strong></li>")
                .append("<li>Rạp / Phòng: <strong>").append(cinemaName).append(" / ").append(auditoriumName).append("</strong></li>")
                .append("<li>Trạng thái: <strong>").append(status.equals("CONFIRMED") ? "Đã xác nhận" : (status.equals("CANCELLED") ? "Đã hủy" : status)).append("</strong></li>")
                .append("</ul>");

        // Danh sách ghế
        sb.append("<h4>Danh sách ghế:</h4>")
                .append("<table><thead><tr><th>Ghế</th><th>Loại</th><th>Giá (VNĐ)</th></tr></thead><tbody>");
        for (OrderTicketItem ticket : order.getTicketItems()) {
            sb.append("<tr>")
                    .append("<td>").append(StringEscapeUtils.escapeXml10(ticket.getSeat().getCode())).append("</td>")
                    .append("<td>").append(StringEscapeUtils.escapeXml10(String.valueOf(ticket.getSeat().getType()))).append("</td>")
                    .append("<td>").append(String.format("%,d", ticket.getPrice())).append("</td>")
                    .append("</tr>");
        }
        sb.append("</tbody></table>");

        // Danh sách dịch vụ
        List<OrderServiceItem> services = order.getServiceItems();
        if (services != null && !services.isEmpty()) {
            sb.append("<h4>Combo/Dịch vụ:</h4>")
                    .append("<table><thead><tr><th>Sản phẩm</th><th>Số lượng</th><th>Giá (VNĐ)</th></tr></thead><tbody>");
            for (OrderServiceItem s : services) {
                sb.append("<tr>")
                        .append("<td>").append(StringEscapeUtils.escapeXml10(s.getAdditionalService().getName())).append("</td>")
                        .append("<td>").append(s.getQuantity()).append("</td>")
                        .append("<td>").append(String.format("%,d", s.getPrice() * s.getQuantity())).append("</td>")
                        .append("</tr>");
            }
            sb.append("</tbody></table>");
        } else {
            sb.append("<p>Không có combo/dịch vụ đi kèm.</p>");
        }

        sb.append("<h4>Tổng giá:</h4>")
                .append("<p>Giảm giá: <strong>").append(String.format("%,d", order.getDiscount() != null ? order.getDiscount() : 0)).append(" VNĐ</strong></p>")
                .append("<p>Tổng thanh toán: <strong>").append(String.format("%,d", totalPrice)).append(" VNĐ</strong></p>")
                .append("</div>")
                .append("<div class='footer'>&amp;copy; 2025 Go Cinema. Mọi quyền được bảo lưu.</div>\n")
                .append("</body></html>");

        return sb.toString();
    }
}
