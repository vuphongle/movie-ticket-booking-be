package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.service.MovieService;

@Slf4j
@RestController
@RequestMapping("api/public/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping("/showing-now")
    public ResponseEntity<?> getShowingNowMovies() {
        return ResponseEntity.ok(movieService.getShowingNowMovies());
    }

    @GetMapping("/coming-soon")
    public ResponseEntity<?> getComingSoonMovies() {
        return ResponseEntity.ok(movieService.getComingSoonMovies());
    }
}
