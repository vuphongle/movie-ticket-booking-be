package vn.edu.iuh.fit.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.model.dto.CinemaRevenueDto;
import vn.edu.iuh.fit.model.dto.MovieRevenueDto;
import vn.edu.iuh.fit.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
  private final DashboardService dashboardService;
  private final UserRepository userRepository;

  public byte[] exportRevenueByCinema(String startDate, String endDate) {
    try {
      List<CinemaRevenueDto> cinemaRevenueDtos =
          dashboardService.getRevenueByCinema(startDate, endDate);

      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Doanh thu theo rạp");

      // Tạo phông chữ tiêu đề
      Font headerFont = workbook.createFont();
      headerFont.setFontName("Arial");
      headerFont.setBold(true);
      headerFont.setFontHeightInPoints((short) 14);

      // Tạo kiểu dáng cell tiêu đề
      CellStyle headerCellStyle = workbook.createCellStyle();
      headerCellStyle.setFont(headerFont);

      // Row 1: Tiêu đề báo cáo
      Row titleRow = sheet.createRow(0);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue("Báo cáo doanh thu theo rạp");
      titleCell.setCellStyle(headerCellStyle);

      // Row 2: Thời gian
      Row dateRow = sheet.createRow(1);
      Cell dateCell = dateRow.createCell(0);
      dateCell.setCellValue("Từ ngày " + startDate + " đến ngày " + endDate);
      dateCell.setCellStyle(headerCellStyle);

      // Row 3: Tiêu đề cột
      Row headerRow = sheet.createRow(2);
      String[] columns = {"Tên rạp", "Số vé bán ra", "Doanh thu"};
      for (int i = 0; i < columns.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerCellStyle);
      }

      // Điền dữ liệu
      int rowNum = 3;
      for (CinemaRevenueDto dto : cinemaRevenueDtos) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(dto.getCinemaName());
        row.createCell(1).setCellValue(dto.getTotalTickets());
        row.createCell(2).setCellValue(dto.getTotalRevenue());
      }

      // Tự động điều chỉnh kích thước cột
      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
      }

      // Ghi dữ liệu vào stream
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      workbook.close();

      return outputStream.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Có lỗi xảy ra khi tạo báo cáo");
    }
  }

  public byte[] exportRevenueByMovie(String startDate, String endDate, String userEmail) {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Doanh thu theo phim");

      // Get user information
      User user = userRepository.findByEmail(userEmail).orElse(null);
      String userName = user != null ? user.getName() : "N/A";
      String userDob = "N/A";
      String userPhone = user != null && user.getPhone() != null ? user.getPhone() : "N/A";

      if (user != null && user.getDob() != null) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        userDob = dateFormat.format(user.getDob());
      }

      String printedBy = String.format("Được in bởi: %s - %s - %s", userName, userDob, userPhone);

      // Parse dates for display
      String displayStartDate = startDate != null ? startDate.replace("-", "/") : "";
      String displayEndDate = endDate != null ? endDate.replace("-", "/") : "";

      // Get current date time for print date (including time)
      java.time.LocalDateTime now = java.time.LocalDateTime.now();
      String printDate =
          String.format(
              "%02d/%02d/%04d %02d:%02d:%02d",
              now.getDayOfMonth(),
              now.getMonthValue(),
              now.getYear(),
              now.getHour(),
              now.getMinute(),
              now.getSecond());

      // Create styles
      CellStyle printDateStyle = createPrintDateStyle(workbook);
      CellStyle titleStyle = createTitleStyleForReport(workbook);
      CellStyle dateRangeStyle = createDateRangeStyle(workbook);
      CellStyle headerStyle = createHeaderStyleForReport(workbook);
      CellStyle centerStyle = createCenterAlignStyle(workbook);
      CellStyle leftStyle = createLeftAlignStyle(workbook);
      CellStyle rightStyle = createRightAlignStyle(workbook);
      CellStyle rightCurrencyStyle = createRightCurrencyStyle(workbook);
      CellStyle totalStyle = createTotalStyle(workbook);
      CellStyle totalCurrencyStyle = createTotalCurrencyStyle(workbook);
      CellStyle totalLabelStyle = createTotalLabelStyle(workbook);

      // Row 1: Print date at top left (A1:G1 merged)
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
      Row printDateRow = sheet.createRow(0);
      Cell printDateCell = printDateRow.createCell(0);
      printDateCell.setCellValue("Ngày in: " + printDate);
      printDateCell.setCellStyle(printDateStyle);

      // Row 2: Printed by (A2:G2 merged)
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));
      Row printedByRow = sheet.createRow(1);
      Cell printedByCell = printedByRow.createCell(0);
      printedByCell.setCellValue(printedBy);
      printedByCell.setCellStyle(printDateStyle);

      // Row 4: Title (A4:G4 merged)
      sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 6));
      Row titleRow = sheet.createRow(3);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue("DOANH THU THEO PHIM");
      titleCell.setCellStyle(titleStyle);

      // Row 5: Date range (A5:G5 merged)
      sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 6));
      Row dateRow = sheet.createRow(4);
      Cell dateCell = dateRow.createCell(0);
      dateCell.setCellValue("Từ ngày: " + displayStartDate + " - Đến ngày: " + displayEndDate);
      dateCell.setCellStyle(dateRangeStyle);

      // Row 7: Headers
      Row headerRow = sheet.createRow(6);
      String[] headers = {
        "STT",
        "Mã phim",
        "Tên phim",
        "Tổng vé bán ra",
        "Chiết khấu",
        "Doanh thu trước CK",
        "Doanh thu sau CK"
      };
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Get data
      List<MovieRevenueDto> movieRevenueDtos =
          dashboardService.getRevenueByMovie(startDate, endDate);

      // Data rows
      int rowNum = 7;
      int stt = 1;
      long totalTickets = 0;
      long totalDiscount = 0;
      long totalRevenueBeforeDiscount = 0;
      long totalRevenueAfterDiscount = 0;

      for (MovieRevenueDto dto : movieRevenueDtos) {
        Row row = sheet.createRow(rowNum++);

        // STT (center)
        Cell sttCell = row.createCell(0);
        sttCell.setCellValue(stt++);
        sttCell.setCellStyle(centerStyle);

        // Mã phim (left)
        Cell codeCell = row.createCell(1);
        codeCell.setCellValue(dto.getMovieCode());
        codeCell.setCellStyle(leftStyle);

        // Tên phim (left)
        Cell nameCell = row.createCell(2);
        nameCell.setCellValue(dto.getMovieName());
        nameCell.setCellStyle(leftStyle);

        // Tổng vé bán ra (right)
        Cell ticketsCell = row.createCell(3);
        ticketsCell.setCellValue(formatNumber(dto.getTotalTickets()));
        ticketsCell.setCellStyle(rightStyle);

        // Chiết khấu (right with currency)
        Cell discountCell = row.createCell(4);
        discountCell.setCellValue(formatCurrency(dto.getTotalDiscount()));
        discountCell.setCellStyle(rightStyle);

        // Doanh thu trước CK (right with currency)
        Cell revenueBeforeCell = row.createCell(5);
        revenueBeforeCell.setCellValue(formatCurrency(dto.getRevenueBeforeDiscount()));
        revenueBeforeCell.setCellStyle(rightStyle);

        // Doanh thu sau CK (right with currency)
        Cell revenueAfterCell = row.createCell(6);
        revenueAfterCell.setCellValue(formatCurrency(dto.getTotalRevenue()));
        revenueAfterCell.setCellStyle(rightStyle);

        // Add borders
        addBordersToRow(row, workbook);

        // Calculate totals
        totalTickets += dto.getTotalTickets();
        totalDiscount += dto.getTotalDiscount();
        totalRevenueBeforeDiscount += dto.getRevenueBeforeDiscount();
        totalRevenueAfterDiscount += dto.getTotalRevenue();
      }

      // Total row (merge A+B)
      Row totalRow = sheet.createRow(rowNum);
      sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 1));

      Cell totalLabelCell = totalRow.createCell(0);
      totalLabelCell.setCellValue("Tổng cộng");
      totalLabelCell.setCellStyle(totalLabelStyle);

      // Empty cell for merged region
      totalRow.createCell(1).setCellStyle(totalLabelStyle);
      totalRow.createCell(2).setCellStyle(totalLabelStyle);

      // Total tickets
      Cell totalTicketsCell = totalRow.createCell(3);
      totalTicketsCell.setCellValue(formatNumber((int) totalTickets));
      totalTicketsCell.setCellStyle(totalStyle);

      // Total discount
      Cell totalDiscountCell = totalRow.createCell(4);
      totalDiscountCell.setCellValue(formatCurrency((int) totalDiscount));
      totalDiscountCell.setCellStyle(totalCurrencyStyle);

      // Total revenue before discount
      Cell totalRevenueBeforeCell = totalRow.createCell(5);
      totalRevenueBeforeCell.setCellValue(formatCurrency((int) totalRevenueBeforeDiscount));
      totalRevenueBeforeCell.setCellStyle(totalCurrencyStyle);

      // Total revenue after discount
      Cell totalRevenueAfterCell = totalRow.createCell(6);
      totalRevenueAfterCell.setCellValue(formatCurrency((int) totalRevenueAfterDiscount));
      totalRevenueAfterCell.setCellStyle(totalCurrencyStyle);

      // Add borders to total row
      addBordersToRow(totalRow, workbook);

      // Auto-size columns to fit content
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
        // Add extra width for better readability
        int currentWidth = sheet.getColumnWidth(i);
        sheet.setColumnWidth(i, currentWidth + 1000);
      }

      // Write to output stream
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);

      return outputStream.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
      throw new BadRequestException("Có lỗi xảy ra khi tạo báo cáo: " + e.getMessage());
    }
  }

  private String formatNumber(int number) {
    return String.format("%,d", number).replace(",", ".");
  }

  private String formatCurrency(int amount) {
    return String.format("%,d VNĐ", amount).replace(",", ".");
  }

  private CellStyle createPrintDateStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.LEFT);
    return style;
  }

  private CellStyle createTitleStyleForReport(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 16);
    font.setBold(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createDateRangeStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 12);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createHeaderStyleForReport(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    font.setBold(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createCenterAlignStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createLeftAlignStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.LEFT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createRightAlignStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createRightCurrencyStyle(Workbook workbook) {
    CellStyle style = createRightAlignStyle(workbook);
    return style;
  }

  private CellStyle createTotalStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    font.setBold(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private CellStyle createTotalCurrencyStyle(Workbook workbook) {
    CellStyle style = createTotalStyle(workbook);
    return style;
  }

  private CellStyle createTotalLabelStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    font.setBold(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.LEFT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private void addBordersToRow(Row row, Workbook workbook) {
    CellStyle borderStyle = workbook.createCellStyle();
    borderStyle.cloneStyleFrom(row.getCell(0).getCellStyle());
    borderStyle.setBorderTop(BorderStyle.THIN);
    borderStyle.setBorderBottom(BorderStyle.THIN);
    borderStyle.setBorderLeft(BorderStyle.THIN);
    borderStyle.setBorderRight(BorderStyle.THIN);

    for (Cell cell : row) {
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.cloneStyleFrom(cell.getCellStyle());
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderBottom(BorderStyle.THIN);
      cellStyle.setBorderLeft(BorderStyle.THIN);
      cellStyle.setBorderRight(BorderStyle.THIN);
      cell.setCellStyle(cellStyle);
    }
  }
}
