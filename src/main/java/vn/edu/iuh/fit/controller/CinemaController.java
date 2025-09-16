package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertCinemaRequest;
import vn.edu.iuh.fit.service.AuditoriumService;
import vn.edu.iuh.fit.service.CinemaService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class CinemaController {
    private final CinemaService cinemaService;
    private final AuditoriumService auditoriumService;
    @GetMapping("/admin/cinemas")
    public ResponseEntity<?> getAllCinemasByAdmin() {
        return ResponseEntity.ok(cinemaService.getAllCinemas());
    }

    @GetMapping("/admin/cinemas/{id}/auditoriums")
    public ResponseEntity<?> getAuditoriumsByCinema(@PathVariable Integer id) {
        return ResponseEntity.ok(auditoriumService.getAuditoriumsByCinema(id));
    }

    @GetMapping("/admin/cinemas/{id}")
    public ResponseEntity<?> getCinemaById(@PathVariable Integer id) {
        return ResponseEntity.ok(cinemaService.getCinemaById(id));
    }

    @PutMapping("/admin/cinemas/{id}")
    public ResponseEntity<?> updateCinema(@PathVariable Integer id, @Valid @RequestBody UpsertCinemaRequest request) {
        return ResponseEntity.ok(cinemaService.updateCinema(id, request));
    }
}
