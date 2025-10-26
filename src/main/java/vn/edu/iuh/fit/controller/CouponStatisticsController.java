package vn.edu.iuh.fit.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.dto.CouponPerformanceDto;
import vn.edu.iuh.fit.model.dto.CouponStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponTypeStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponUsageDto;
import vn.edu.iuh.fit.service.CouponStatisticsService;

/** Controller for coupon statistics and reporting */
@Slf4j
@RestController
@RequestMapping("api/admin/coupon-statistics")
@RequiredArgsConstructor
public class CouponStatisticsController {

  private final CouponStatisticsService couponStatisticsService;

  /**
   * Get overall coupon statistics (dashboard)
   *
   * @param startDate Start date (optional, format: yyyy-MM-dd)
   * @param endDate End date (optional, format: yyyy-MM-dd)
   * @return Overall statistics
   */
  @GetMapping("")
  public ResponseEntity<CouponStatisticsDto> getStatistics(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    log.info("Getting coupon statistics from {} to {}", startDate, endDate);
    CouponStatisticsDto stats = couponStatisticsService.getStatistics(startDate, endDate);
    return ResponseEntity.ok(stats);
  }

  /**
   * Get performance statistics for all coupons
   *
   * @param startDate Start date (optional)
   * @param endDate End date (optional)
   * @return List of coupon performances
   */
  @GetMapping("/performance")
  public ResponseEntity<List<CouponPerformanceDto>> getPerformance(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    log.info("Getting coupon performance from {} to {}", startDate, endDate);
    List<CouponPerformanceDto> performances =
        couponStatisticsService.getPerformance(startDate, endDate);
    return ResponseEntity.ok(performances);
  }

  /**
   * Get performance statistics for a specific coupon
   *
   * @param couponId Coupon ID
   * @param startDate Start date (optional)
   * @param endDate End date (optional)
   * @return Coupon performance
   */
  @GetMapping("/performance/{couponId}")
  public ResponseEntity<CouponPerformanceDto> getPerformanceById(
      @PathVariable Integer couponId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    log.info("Getting performance for coupon {} from {} to {}", couponId, startDate, endDate);
    CouponPerformanceDto performance =
        couponStatisticsService.getPerformanceById(couponId, startDate, endDate);
    return ResponseEntity.ok(performance);
  }

  /**
   * Get coupon usage history
   *
   * @param couponId Coupon ID (optional, if null returns all)
   * @param startDate Start date (optional)
   * @param endDate End date (optional)
   * @return List of usage records
   */
  @GetMapping("/usage")
  public ResponseEntity<List<CouponUsageDto>> getUsageHistory(
      @RequestParam(required = false) Integer couponId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    log.info(
        "Getting coupon usage history for coupon {} from {} to {}", couponId, startDate, endDate);
    List<CouponUsageDto> usages =
        couponStatisticsService.getUsageHistory(couponId, startDate, endDate);
    return ResponseEntity.ok(usages);
  }

  /**
   * Get statistics by coupon type (benefit type and target type)
   *
   * @param startDate Start date (optional)
   * @param endDate End date (optional)
   * @return List of type statistics
   */
  @GetMapping("/by-type")
  public ResponseEntity<List<CouponTypeStatisticsDto>> getTypeStatistics(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    log.info("Getting coupon type statistics from {} to {}", startDate, endDate);
    List<CouponTypeStatisticsDto> typeStats =
        couponStatisticsService.getTypeStatistics(startDate, endDate);
    return ResponseEntity.ok(typeStats);
  }

  /**
   * Export coupon performance report to Excel
   *
   * @param startDate Start date (optional)
   * @param endDate End date (optional)
   * @param principal Current user
   * @return Excel file
   */
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportReport(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Principal principal) {
    log.info("Exporting coupon performance report from {} to {}", startDate, endDate);

    try {
      String userEmail = principal.getName();
      byte[] excelData = couponStatisticsService.exportPerformance(startDate, endDate, userEmail);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(
          MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
      headers.setContentDispositionFormData(
          "attachment", "Thong_ke_khuyen_mai_" + System.currentTimeMillis() + ".xlsx");
      headers.setContentLength(excelData.length);

      return ResponseEntity.ok().headers(headers).body(excelData);
    } catch (IOException e) {
      log.error("Error exporting coupon performance report", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
