package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Review;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertReviewRequest;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ReviewRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;

    public Page<Review> getAllReviewsByMovieId(Integer movieId, Integer page, Integer limit) {
        if (!movieRepository.existsByIdAndStatus(movieId, true)) {
            throw new ResourceNotFoundException("Movie not found");
        }
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        return reviewRepository.findByMovie_Id(movieId, pageable);
    }

    private void updateRatingOfMovie(Movie movie) {
        List<Review> reviews = reviewRepository.findByMovie_Id(movie.getId());
        double rating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0);
        rating = Math.round(rating * 10) / 10.0;
        movie.setRating(rating);
        movieRepository.save(movie);
    }

    @Transactional
    public Review adminUpdateReview(UpsertReviewRequest request, Integer id) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim có id = " + request.getMovieId()));

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review có id = " + id));

        // check review is for movie
        if (!review.getMovie().getId().equals(movie.getId())) {
            throw new BadRequestException("Review không thuộc phim này");
        }

        review.setComment(request.getComment());
        review.setRating(request.getRating());
        review.setMovie(movie);

        reviewRepository.save(review);

        // update rating of movie
        updateRatingOfMovie(movie);

        return review;
    }

    @Transactional
    public void adminDeleteReview(Integer id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review có id = " + id));
        reviewRepository.delete(review);

        // update rating of movie
        updateRatingOfMovie(review.getMovie());
    }
}
