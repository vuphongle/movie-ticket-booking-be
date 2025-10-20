package vn.edu.iuh.fit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Review;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertReviewRequest;
import vn.edu.iuh.fit.model.response.ImageResponse;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ReviewRepository;
import vn.edu.iuh.fit.security.SecurityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
  private final MovieRepository movieRepository;
  private final ReviewRepository reviewRepository;
  private final ImageService imageService;

  public Page<Review> getAllReviewsByMovieId(Integer movieId, Integer page, Integer limit) {
    if (!movieRepository.existsByIdAndStatus(movieId, true)) {
      throw new ResourceNotFoundException("Movie not found");
    }
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
    return reviewRepository.findByMovie_Id(movieId, pageable);
  }

  @Transactional
  public Review createReview(UpsertReviewRequest request, List<MultipartFile> files) {

    Optional<Review> existing =
        reviewRepository.findByUser_IdAndMovie_Id(
            Integer.valueOf(request.getUserId()), request.getMovieId());
    if (existing.isPresent()) {
      throw new BadRequestException("Bạn đã đánh giá phim này rồi");
    }

    User user = SecurityUtils.getCurrentUserLogin();

    Movie movie =
        movieRepository
            .findById(request.getMovieId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phim có id = " + request.getMovieId()));

    List<String> images = new ArrayList<>();
    if (files != null && !files.isEmpty()) {
      for (MultipartFile file : files) {
        ImageResponse imageResponse = imageService.uploadImage(file);
        images.add(imageResponse.getUrl());
      }
    }

    Review review =
        Review.builder()
            .user(user)
            .comment(request.getComment())
            .rating(request.getRating())
            .feeling(request.getFeeling())
            .images(images)
            .movie(movie)
            .build();
    reviewRepository.save(review);

    // update rating of movie
    updateRatingOfMovie(movie);

    return review;
  }

  // Xóa 1 review của user về 1 phim
  @Transactional
  public void deleteReviewByUser(Integer reviewId) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy review có id = " + reviewId));
    reviewRepository.delete(review);

    // update rating of movie
    updateRatingOfMovie(review.getMovie());
  }

  // Cập nhật đánh giá của user về 1 phim
  @Transactional
  public Review updateReviewByUser(UpsertReviewRequest request, List<MultipartFile> files) {
    User user = SecurityUtils.getCurrentUserLogin();

    Movie movie =
        movieRepository
            .findById(request.getMovieId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phim có id = " + request.getMovieId()));

    Review review =
        reviewRepository
            .findByUser_IdAndMovie_Id(user.getId(), request.getMovieId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy review của user về phim này"));

    review.setComment(request.getComment());
    review.setRating(request.getRating());
    review.setFeeling(request.getFeeling());

    // Nếu có file ảnh mới, upload lên S3
    if (files != null && !files.isEmpty()) {
      List<String> images = new ArrayList<>();
      for (MultipartFile file : files) {
        ImageResponse imageResponse = imageService.uploadImage(file);
        images.add(imageResponse.getUrl());
      }
      review.setImages(images);
    }

    reviewRepository.save(review);

    // update rating of movie
    updateRatingOfMovie(movie);

    return review;
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
    Movie movie =
        movieRepository
            .findById(request.getMovieId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phim có id = " + request.getMovieId()));

    Review review =
        reviewRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy review có id = " + id));

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
    Review review =
        reviewRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy review có id = " + id));
    reviewRepository.delete(review);

    // update rating of movie
    updateRatingOfMovie(review.getMovie());
  }
}
