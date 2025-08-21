package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Review;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ReviewRepository;

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
}
