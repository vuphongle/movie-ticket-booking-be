package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
