package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertReviewRequest;
import vn.edu.iuh.fit.service.MovieService;
import vn.edu.iuh.fit.service.ReviewService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final MovieService movieService;

    @GetMapping("/public/reviews")
    public ResponseEntity<?> getAllReviewsOfMovies(@RequestParam(required = false, defaultValue = "1") Integer page,
                                                   @RequestParam(required = false, defaultValue = "6") Integer limit) {
        return ResponseEntity.ok(movieService.getAllReviewsOfMovies(page, limit));
    }

    @PutMapping("/admin/reviews/{id}")
    public ResponseEntity<?> adminUpdateReview(@Valid @RequestBody UpsertReviewRequest request,
                                               @PathVariable Integer id) {
        return ResponseEntity.ok(reviewService.adminUpdateReview(request, id));
    }

    @DeleteMapping("/admin/reviews/{id}")
    public ResponseEntity<?> adminDeleteReview(@PathVariable Integer id) {
        reviewService.adminDeleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
