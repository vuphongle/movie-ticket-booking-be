package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.Movie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.service.MovieService;
import vn.edu.iuh.fit.service.ShowtimeService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    private final ShowtimeService showtimeService;

    @GetMapping("/public/movies/showing-now")
    public ResponseEntity<?> getShowingNowMovies() {
        return ResponseEntity.ok(movieService.getShowingNowMovies());
    }

    @GetMapping("/public/movies/coming-soon")
    public ResponseEntity<?> getComingSoonMovies() {
        return ResponseEntity.ok(movieService.getComingSoonMovies());
    }

    @GetMapping("/public/movies/{id}/{slug}")
    public ResponseEntity<?> getMovieDetail(@PathVariable Integer id, @PathVariable String slug) {
        return ResponseEntity.ok(movieService.getMovieDetail(id, slug));
    }

    @GetMapping("/public/movies/{id}/showtimes")
    public ResponseEntity<?> getShowtimesByMovie(@PathVariable Integer id, @RequestParam String showDate) {
        return ResponseEntity.ok(showtimeService.getShowtimesByMovie(id, showDate));
    }

    @GetMapping("/public/movies/{id}/has-showtimes")
    public ResponseEntity<?> checkMovieHasShowtimes(@PathVariable Integer id) {
        return ResponseEntity.ok(showtimeService.checkMovieHasShowtimes(id));
    }

    @GetMapping("/public/movie-by-showtimeId/{id}")
    public Movie getMovieByShowtime(@PathVariable Integer id) {
        return showtimeService.getMovieByShowtimeId(id);
    }
    
    @GetMapping("/admin/movies/in-schedule")
    public ResponseEntity<?> getAllMoviesInSchedule(@RequestParam String date) {
        return ResponseEntity.ok(movieService.getAllMoviesInSchedule(date));
    }
}
