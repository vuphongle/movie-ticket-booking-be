package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertScheduleRequest;
import vn.edu.iuh.fit.service.ScheduleService;

@Slf4j
@RestController
@RequestMapping("api/admin/schedules")
@RequiredArgsConstructor
public class ScheduleController {
  private final ScheduleService scheduleService;

  @GetMapping
  public ResponseEntity<?> getAllSchedules() {
    return ResponseEntity.ok(scheduleService.getAllSchedules());
  }

  @PostMapping
  public ResponseEntity<?> createSchedule(@Valid @RequestBody UpsertScheduleRequest request) {
    return new ResponseEntity<>(scheduleService.saveSchedule(request), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteSchedule(@PathVariable Integer id) {
    scheduleService.deleteSchedule(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updateSchedule(
      @PathVariable Integer id, @Valid @RequestBody UpsertScheduleRequest request) {
    return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
  }
}
