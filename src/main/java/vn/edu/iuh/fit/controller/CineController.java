package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.service.CineService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class CineController {
    private final CineService cineService;

    @GetMapping("/public/cinemas/getAllNames")
    public ResponseEntity<?> getShowingNowMovies() {
        return ResponseEntity.ok(cineService.getAllCinemaNames());
    }

    @GetMapping("/public/cinemas/getAllCities")
    public ResponseEntity<?> getAllCities() {
        return ResponseEntity.ok(cineService.getAllCities());
    }

}
