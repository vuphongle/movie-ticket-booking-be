package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Review;
import vn.edu.iuh.fit.entity.Schedule;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ScheduleRepository;

import java.util.*;

@Slf4j
@Service
public class MovieService {
    private final MovieRepository movieRepository;
    private final ScheduleRepository scheduleRepository;

    public MovieService(MovieRepository movieRepository, ScheduleRepository scheduleRepository) {
        this.movieRepository = movieRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<Movie> getShowingNowMovies() {
        log.info("Get showing now movies");
        List<Schedule> schedules = scheduleRepository.findByMovie_StatusAndStartDateBeforeAndEndDateAfter(true, new Date(), new Date());
        return schedules.stream().map(Schedule::getMovie).toList();
    }

    public List<Movie> getComingSoonMovies() {
        log.info("Get coming soon movies");
        List<Schedule> schedules = scheduleRepository.findByMovie_StatusAndStartDateAfter(true, new Date());
        return schedules.stream().map(Schedule::getMovie).toList();
    }

    public Page<Movie> getAllReviewsOfMovies(Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Movie> pageData = movieRepository.findByStatus(true, pageable);

        pageData.getContent().forEach(movie -> {
            Set<Review> reviews = movie.getReviews();
            List<Review> sortedReviews = reviews.stream()
                    .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                    .toList();
            movie.setReviews(new LinkedHashSet<>(sortedReviews));
        });

        return pageData;
    }
}
