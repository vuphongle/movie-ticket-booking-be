package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertAuditorium;
import vn.edu.iuh.fit.service.AuditoriumService;
import vn.edu.iuh.fit.service.SeatService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class AuditoriumController {
    private final SeatService seatService;
    private final AuditoriumService auditoriumService;

    @GetMapping("/public/auditoriums/{auditoriumId}/showtimes/{showtimeId}/seats")
    ResponseEntity<?> getSeatsByAuditoriumAndShowtime(@PathVariable Integer auditoriumId,
                                                      @PathVariable Integer showtimeId) {
        return ResponseEntity.ok(seatService.getSeatsByAuditoriumAndShowtime(auditoriumId, showtimeId));
    }

    @PostMapping("/admin/auditoriums")
    public ResponseEntity<?> createAuditorium(@Valid @RequestBody UpsertAuditorium request) {
        return new ResponseEntity<>(auditoriumService.saveAuditorium(request), HttpStatus.CREATED);
    }

    @PutMapping("/admin/auditoriums/{id}")
    public ResponseEntity<?> updateAuditorium(@PathVariable Integer id, @Valid @RequestBody UpsertAuditorium request) {
        return ResponseEntity.ok(auditoriumService.updateAuditorium(id, request));
    }

    @DeleteMapping("/admin/auditoriums/{id}")
    public ResponseEntity<?> deleteAuditorium(@PathVariable Integer id) {
        auditoriumService.deleteAuditorium(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/auditoriums/{id}/seats")
    public ResponseEntity<?> getSeatsByAuditorium(@PathVariable Integer id) {
        return ResponseEntity.ok(auditoriumService.getSeatsByAuditorium(id));
    }
}