package vn.edu.iuh.fit.controller;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.service.DashboardService;
import vn.edu.iuh.fit.service.ReportService;

@Slf4j
@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboardService;
  private final ReportService reportService;

  @GetMapping("/dashboard")
  public ResponseEntity<?> getDashboard(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(dashboardService.getDashboardData(startDate, endDate));
  }

  @GetMapping("/revenue/cinema")
  public ResponseEntity<?> getRevenueByCinema(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(dashboardService.getRevenueByCinema(startDate, endDate));
  }

  @GetMapping("/revenue/cinema/export")
  public ResponseEntity<?> exportRevenueByCinema(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Principal principal) {
    byte[] data = reportService.exportRevenueByCinema(startDate, endDate, principal.getName());

    String currentDate =
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String filename = "Revenue_Report_Cinema_" + currentDate + ".xlsx";

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
  }

  @GetMapping("/revenue/movie")
  public ResponseEntity<?> getRevenueByMovie(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(dashboardService.getRevenueByMovie(startDate, endDate));
  }

  @GetMapping("/revenue/movie/export")
  public ResponseEntity<?> exportRevenueByMovie(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Principal principal) {
    byte[] data = reportService.exportRevenueByMovie(startDate, endDate, principal.getName());

    String currentDate =
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String filename = "Revenue_Report_Movie_" + currentDate + ".xlsx";

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
  }

  @GetMapping("/revenue/movie/{movieId}")
  public ResponseEntity<?> getRevenueByMovieId(
      @PathVariable Integer movieId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(dashboardService.getRevenueByMovieId(movieId, startDate, endDate));
  }

  @GetMapping("/revenue/movie/{movieId}/export")
  public ResponseEntity<?> exportRevenueByMovieId(
      @PathVariable Integer movieId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Principal principal) {
    byte[] data =
        reportService.exportRevenueByMovieId(movieId, startDate, endDate, principal.getName());

    String currentDate =
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String filename = "Revenue_Report_Movie_" + movieId + "_" + currentDate + ".xlsx";

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
  }

  @GetMapping("/revenue/cinema/{cinemaId}")
  public ResponseEntity<?> getRevenueByCinemaId(
      @PathVariable Integer cinemaId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(dashboardService.getRevenueByCinemaId(cinemaId, startDate, endDate));
  }

  @GetMapping("/revenue/cinema/{cinemaId}/export")
  public ResponseEntity<?> exportRevenueByCinemaId(
      @PathVariable Integer cinemaId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Principal principal) {
    byte[] data =
        reportService.exportRevenueByCinemaId(cinemaId, startDate, endDate, principal.getName());

    String currentDate =
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String filename = "Revenue_Report_Cinema_" + cinemaId + "_" + currentDate + ".xlsx";

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
  }

  @GetMapping("/revenue/customer")
  public ResponseEntity<?> getRevenueByCustomer(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(dashboardService.getRevenueByCustomer(startDate, endDate));
  }

  @GetMapping("/revenue/customer/export")
  public ResponseEntity<?> exportRevenueByCustomer(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Principal principal) {
    byte[] data = reportService.exportRevenueByCustomer(startDate, endDate, principal.getName());

    String currentDate =
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String filename = "Revenue_Report_Customer_" + currentDate + ".xlsx";

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
  }

  @GetMapping("/revenue/customer/{customerId}")
  public ResponseEntity<?> getRevenueByCustomerId(
      @PathVariable Integer customerId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(
        dashboardService.getRevenueByCustomerId(customerId, startDate, endDate));
  }

  @GetMapping("/revenue/customer/{customerId}/export")
  public ResponseEntity<?> exportRevenueByCustomerId(
      @PathVariable Integer customerId,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Principal principal) {
    byte[] data =
        reportService.exportRevenueByCustomerId(
            customerId, startDate, endDate, principal.getName());

    String currentDate =
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String filename = "Revenue_Report_Customer_" + customerId + "_" + currentDate + ".xlsx";

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
  }
}
