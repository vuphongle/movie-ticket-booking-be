package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpdateRowSeatRequest;
import vn.edu.iuh.fit.model.request.UpsertSeatRequest;
import vn.edu.iuh.fit.service.SeatService;

@Slf4j
@RestController
@RequestMapping("api/admin/seats")
@RequiredArgsConstructor
public class SeatController {
  private final SeatService seatService;

  @PutMapping("/{id}")
  public ResponseEntity<?> updateSeat(
      @PathVariable Integer id, @Valid @RequestBody UpsertSeatRequest request) {
    return ResponseEntity.ok(seatService.updateSeat(id, request));
  }

  @PutMapping("/update-row-seats")
  public ResponseEntity<?> updateSeat(@Valid @RequestBody UpdateRowSeatRequest request) {
    seatService.updateRowSeat(request);
    return ResponseEntity.ok().build();
  }
}
