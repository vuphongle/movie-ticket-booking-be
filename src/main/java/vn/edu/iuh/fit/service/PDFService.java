package vn.edu.iuh.fit.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Order;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class PDFService {

    private static final String STORAGE_DIR = "pdf-orders";

    public String generateOrderPdf(Order order, String qrCodeBase64) throws Exception {
        Path path = Paths.get(STORAGE_DIR);
        if (!Files.exists(path)) Files.createDirectories(path);

        String fileName = "order-" + order.getId() + ".pdf";
        Path filePath = path.resolve(fileName);

        // HTML template với các biến
        String html = buildHtml(order, qrCodeBase64);

        try (FileOutputStream os = new FileOutputStream(filePath.toFile())) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
        }

        return filePath.toString();
    }

    private String buildHtml(Order order, String qrCodeBase64) {
        // Chèn dữ liệu vào template HTML
        String qrImg = qrCodeBase64 != null ? "data:image/png;base64," + qrCodeBase64 : "";
        String htmlTemplate = "<!DOCTYPE html><html>..."; // copy toàn bộ HTML bạn gửi
        htmlTemplate = htmlTemplate.replace("${userName}", order.getUser().getName())
                .replace("${userPhone}", order.getUser().getPhone())
                .replace("${orderId}", order.getId().toString())
                .replace("${movieTitle}", order.getShowtime().getMovie().getName()
                .replace("${cinemaName}", order.getShowtime().getAuditorium().getCinema().getName())
                .replace("${auditoriumName}", order.getShowtime().getAuditorium().getName())
                .replace("${showDate}", order.getShowtime().getDate().toString())
                .replace("${startTime}", order.getShowtime().getStartTime().toString())
                .replace("${endTime}", order.getShowtime().getEndTime().toString())
                .replace("${status}", order.getStatus().name())
                .replace("${qrCodePath}", qrImg));

        // TODO: chèn danh sách ghế, dịch vụ, coupon, tổng tiền, giảm giá
        // Có thể dùng StringBuilder hoặc template engine như Thymeleaf để loop danh sách
        return htmlTemplate;
    }
}

