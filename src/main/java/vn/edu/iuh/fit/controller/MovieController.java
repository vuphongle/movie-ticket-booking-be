package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.Movie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.model.dto.MovieWithShowtimesDto;
import vn.edu.iuh.fit.model.request.UpsertMovieRequest;
import vn.edu.iuh.fit.service.MovieService;
import vn.edu.iuh.fit.service.ShowtimeService;

import java.util.List;

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

    @GetMapping("/public/cinemas/{cinemaId}/movies-showtimes")
    public ResponseEntity<List<MovieWithShowtimesDto>> getMoviesWithShowtimesByCinema(
            @PathVariable Integer cinemaId) {
        return ResponseEntity.ok(showtimeService.getShowtimesByCinema(cinemaId));
    }

    @GetMapping("/public/cinemas/{cinemaName}/movies-showtimes-by-cinema-name")
    public ResponseEntity<List<MovieWithShowtimesDto>> getMoviesWithShowtimesByCinemaName(
            @PathVariable String cinemaName) {
        return ResponseEntity.ok(showtimeService.getShowtimesByCinemaName(cinemaName));
    }

    @GetMapping("/public/movies/search")
    public ResponseEntity<?> searchMovies(@RequestParam String keyword) {
        return ResponseEntity.ok(movieService.searchShowingOrComingMovies(keyword));
    }

    @GetMapping("/admin/movies/in-schedule")
    public ResponseEntity<?> getAllMoviesInSchedule(@RequestParam String date) {
        return ResponseEntity.ok(movieService.getAllMoviesInSchedule(date));
    }

    @GetMapping("/admin/movies")
    public ResponseEntity<?> getAllMovies(@RequestParam(required = false) Boolean status) {
        return ResponseEntity.ok(movieService.getAllMovies(status));
    }

    @PostMapping("/admin/movies")
    public ResponseEntity<?> createMovie(@Valid @RequestBody UpsertMovieRequest request) {
        return new ResponseEntity<>(movieService.saveMovie(request), HttpStatus.CREATED);
    }

    @GetMapping("/admin/movies/{id}")
    public ResponseEntity<?> getMovieById(@PathVariable Integer id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @PutMapping("/admin/movies/{id}")
    public ResponseEntity<?> updateMovie(@PathVariable Integer id, @Valid @RequestBody UpsertMovieRequest request) {
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @DeleteMapping("/admin/movies/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable Integer id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
