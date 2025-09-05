package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.service.SeatService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class AuditoriumController {
    private final SeatService seatService;

    @GetMapping("/public/auditoriums/{auditoriumId}/showtimes/{showtimeId}/seats")
    ResponseEntity<?> getSeatsByAuditoriumAndShowtime(@PathVariable Integer auditoriumId,
                                                      @PathVariable Integer showtimeId) {
        return ResponseEntity.ok(seatService.getSeatsByAuditoriumAndShowtime(auditoriumId, showtimeId));
    }
}