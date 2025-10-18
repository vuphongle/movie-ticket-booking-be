package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.BulkShowtimeRequest;
import vn.edu.iuh.fit.model.request.UpsertShowtimeRequest;
import vn.edu.iuh.fit.service.ShowtimeService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class ShowTimeController {
  private final ShowtimeService showtimeService;

  @GetMapping("/admin/showtimes")
  public ResponseEntity<?> getAllShowtimes(
      @RequestParam(required = false) Integer cinemaId,
      @RequestParam(required = false) Integer auditoriumId,
      @RequestParam String showDate) {
    return ResponseEntity.ok(showtimeService.getAllShowtimes(cinemaId, auditoriumId, showDate));
  }

  @PostMapping("/admin/showtimes")
  public ResponseEntity<?> createShowtimes(@Valid @RequestBody UpsertShowtimeRequest request) {
    return ResponseEntity.ok(showtimeService.createShowtimes(request));
  }

  @PostMapping("/admin/showtimes/bulk")
  public ResponseEntity<?> createBulkShowtimes(@Valid @RequestBody BulkShowtimeRequest request) {
    return ResponseEntity.ok(showtimeService.createBulkShowtimes(request));
  }
}
