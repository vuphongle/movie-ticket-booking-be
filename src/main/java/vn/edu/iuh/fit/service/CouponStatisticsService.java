package vn.edu.iuh.fit.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.model.dto.CouponPerformanceDto;
import vn.edu.iuh.fit.model.dto.CouponStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponTypeStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponUsageDto;
import vn.edu.iuh.fit.repository.CouponRepository;
import vn.edu.iuh.fit.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponStatisticsService {

  private final CouponRepository couponRepository;
  private final UserRepository userRepository;

  /** Get overall coupon statistics */
  public CouponStatisticsDto getStatistics(String startDate, String endDate) {
    LocalDateTime start = parseDate(startDate, true);
    LocalDateTime end = parseDate(endDate, false);

    return couponRepository.getCouponStatistics(start, end);
  }

  /** Get performance statistics for all coupons */
  public List<CouponPerformanceDto> getPerformance(String startDate, String endDate) {
    LocalDateTime start = parseDate(startDate, true);
    LocalDateTime end = parseDate(endDate, false);

    return couponRepository.getCouponPerformance(start, end);
  }

  /** Get performance statistics for a specific coupon */
  public CouponPerformanceDto getPerformanceById(
      Integer couponId, String startDate, String endDate) {
    LocalDateTime start = parseDate(startDate, true);
    LocalDateTime end = parseDate(endDate, false);

    return couponRepository.getCouponPerformanceById(couponId, start, end);
  }

  /** Get coupon usage history */
  public List<CouponUsageDto> getUsageHistory(Integer couponId, String startDate, String endDate) {
    LocalDateTime start = parseDate(startDate, true);
    LocalDateTime end = parseDate(endDate, false);

    return couponRepository.getCouponUsageHistory(couponId, start, end);
  }

  /** Get statistics by coupon type */
  public List<CouponTypeStatisticsDto> getTypeStatistics(String startDate, String endDate) {
    LocalDateTime start = parseDate(startDate, true);
    LocalDateTime end = parseDate(endDate, false);

    return couponRepository.getCouponTypeStatistics(start, end);
  }

  /** Export coupon performance to Excel */
  public byte[] exportPerformance(String startDate, String endDate, String userEmail)
      throws IOException {
    List<CouponPerformanceDto> performances = getPerformance(startDate, endDate);

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Thống kê khuyến mại");
      sheet.setDisplayGridlines(false);

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

      // Get current date time for print date
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
      CellStyle titleStyle = createTitleStyle(workbook);
      CellStyle dateRangeStyle = createDateRangeStyle(workbook);
      CellStyle headerStyle = createHeaderStyle(workbook);
      CellStyle centerStyle = createCenterStyle(workbook);
      CellStyle currencyStyle = createCurrencyStyle(workbook);
      CellStyle totalStyle = createTotalStyle(workbook);
      CellStyle totalCurrencyStyle = createTotalCurrencyStyle(workbook);

      // Row 1: Print date (A1:L1 merged)
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));
      Row printDateRow = sheet.createRow(0);
      Cell printDateCell = printDateRow.createCell(0);
      printDateCell.setCellValue("Ngày in: " + printDate);
      printDateCell.setCellStyle(printDateStyle);

      // Row 2: Printed by (A2:L2 merged)
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 11));
      Row printedByRow = sheet.createRow(1);
      Cell printedByCell = printedByRow.createCell(0);
      printedByCell.setCellValue(printedBy);
      printedByCell.setCellStyle(printDateStyle);

      // Row 4: Title (A4:L4 merged)
      sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 11));
      Row titleRow = sheet.createRow(3);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue("THỐNG KÊ HIỆU SUẤT KHUYẾN MẠI");
      titleCell.setCellStyle(titleStyle);

      // Row 5: Date range (A5:L5 merged)
      sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 11));
      Row dateRow = sheet.createRow(4);
      Cell dateCell = dateRow.createCell(0);
      dateCell.setCellValue("Từ ngày: " + displayStartDate + " - Đến ngày: " + displayEndDate);
      dateCell.setCellStyle(dateRangeStyle);

      // Row 7: Headers
      Row headerRow = sheet.createRow(6);
      String[] headers = {
        "STT",
        "Mã KM",
        "Tên khuyến mại",
        "Loại",
        "Thời gian HĐ",
        "Số lần SD",
        "Số KH",
        "Tổng giảm giá",
        "DT trước CK",
        "DT sau CK",
        "Tỷ lệ giảm (%)",
        "Trạng thái"
      };

      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      // Data rows
      int rowNum = 7;
      int stt = 1;
      int totalUsage = 0;
      int totalCustomers = 0;
      int totalDiscount = 0;
      int totalRevBefore = 0;
      int totalRevAfter = 0;

      for (CouponPerformanceDto perf : performances) {
        Row row = sheet.createRow(rowNum++);

        // STT
        Cell sttCell = row.createCell(0);
        sttCell.setCellValue(stt++);
        sttCell.setCellStyle(centerStyle);

        // Mã KM
        Cell codeCell = row.createCell(1);
        codeCell.setCellValue(perf.getCouponCode() != null ? perf.getCouponCode() : "N/A");
        codeCell.setCellStyle(centerStyle);

        // Tên
        Cell nameCell = row.createCell(2);
        nameCell.setCellValue(perf.getCouponName());

        // Loại
        Cell kindCell = row.createCell(3);
        kindCell.setCellValue(perf.getKind());
        kindCell.setCellStyle(centerStyle);

        // Thời gian
        Cell dateCell2 = row.createCell(4);
        String dateRange = formatDateRange(perf.getStartDate(), perf.getEndDate());
        dateCell2.setCellValue(dateRange);
        dateCell2.setCellStyle(centerStyle);

        // Số lần SD
        Cell usageCell = row.createCell(5);
        usageCell.setCellValue(perf.getUsageCount());
        usageCell.setCellStyle(centerStyle);
        totalUsage += perf.getUsageCount();

        // Số KH
        Cell custCell = row.createCell(6);
        custCell.setCellValue(perf.getUniqueCustomers());
        custCell.setCellStyle(centerStyle);
        totalCustomers += perf.getUniqueCustomers();

        // Tổng giảm giá
        Cell discountCell = row.createCell(7);
        discountCell.setCellValue(formatCurrency(perf.getTotalDiscountValue()));
        discountCell.setCellStyle(currencyStyle);
        totalDiscount += perf.getTotalDiscountValue();

        // DT trước CK
        Cell revBeforeCell = row.createCell(8);
        revBeforeCell.setCellValue(formatCurrency(perf.getRevenueBeforeDiscount()));
        revBeforeCell.setCellStyle(currencyStyle);
        totalRevBefore += perf.getRevenueBeforeDiscount();

        // DT sau CK
        Cell revAfterCell = row.createCell(9);
        revAfterCell.setCellValue(formatCurrency(perf.getRevenueWithCoupon()));
        revAfterCell.setCellStyle(currencyStyle);
        totalRevAfter += perf.getRevenueWithCoupon();

        // Tỷ lệ giảm
        Cell rateCell = row.createCell(10);
        rateCell.setCellValue(
            String.format(
                "%.2f%%",
                perf.getDiscountPercentage() != null ? perf.getDiscountPercentage() : 0.0));
        rateCell.setCellStyle(centerStyle);

        // Trạng thái
        Cell statusCell = row.createCell(11);
        statusCell.setCellValue(perf.getStatusLabel());
        statusCell.setCellStyle(centerStyle);
      }

      // Total row
      Row totalRow = sheet.createRow(rowNum);

      // Label
      sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 4));
      Cell totalLabelCell = totalRow.createCell(0);
      totalLabelCell.setCellValue("TỔNG CỘNG");
      totalLabelCell.setCellStyle(totalStyle);

      // Total usage
      Cell totalUsageCell = totalRow.createCell(5);
      totalUsageCell.setCellValue(totalUsage);
      totalUsageCell.setCellStyle(totalStyle);

      // Total customers
      Cell totalCustCell = totalRow.createCell(6);
      totalCustCell.setCellValue(totalCustomers);
      totalCustCell.setCellStyle(totalStyle);

      // Total discount
      Cell totalDiscCell = totalRow.createCell(7);
      totalDiscCell.setCellValue(formatCurrency(totalDiscount));
      totalDiscCell.setCellStyle(totalCurrencyStyle);

      // Total rev before
      Cell totalRevBeforeCell = totalRow.createCell(8);
      totalRevBeforeCell.setCellValue(formatCurrency(totalRevBefore));
      totalRevBeforeCell.setCellStyle(totalCurrencyStyle);

      // Total rev after
      Cell totalRevAfterCell = totalRow.createCell(9);
      totalRevAfterCell.setCellValue(formatCurrency(totalRevAfter));
      totalRevAfterCell.setCellStyle(totalCurrencyStyle);

      // Auto-size columns
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
        int currentWidth = sheet.getColumnWidth(i);
        sheet.setColumnWidth(i, currentWidth + 1000);
      }

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  // Helper methods
  private LocalDateTime parseDate(String dateStr, boolean isStart) {
    if (dateStr == null || dateStr.isEmpty()) {
      return isStart
          ? LocalDate.now().minusMonths(1).atStartOfDay()
          : LocalDate.now().atTime(23, 59, 59);
    }
    LocalDate date = LocalDate.parse(dateStr);
    return isStart ? date.atStartOfDay() : date.atTime(23, 59, 59);
  }

  private String formatDateRange(LocalDateTime start, LocalDateTime end) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    return start.format(formatter) + " - " + end.format(formatter);
  }

  private String formatCurrency(Integer amount) {
    if (amount == null) return "0";
    return String.format("%,d", amount);
  }

  // Excel style methods (similar to ReportService)
  private CellStyle createPrintDateStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontHeightInPoints((short) 10);
    font.setItalic(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.LEFT);
    return style;
  }

  private CellStyle createTitleStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 16);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createDateRangeStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    return style;
  }

  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 11);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createCenterStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createCurrencyStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createTotalStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private CellStyle createTotalCurrencyStyle(Workbook workbook) {
    CellStyle style = createTotalStyle(workbook);
    style.setAlignment(HorizontalAlignment.RIGHT);
    return style;
  }
}
