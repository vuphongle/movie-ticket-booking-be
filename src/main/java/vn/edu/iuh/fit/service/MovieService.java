package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Schedule;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ScheduleRepository;

import java.util.Date;
import java.util.List;

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

    public List<Movie> getAllMovies(Boolean status) {
        log.info("Get all movies");
        if (status != null) {
            return movieRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return movieRepository.findAll(Sort.by("createdAt").descending());
    }


    public Page<Movie> getAllMovies(Integer page, Integer limit) {
        log.info("Get all movies");
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("showDate").descending());
        return movieRepository.findByStatus(true, pageable);
    }
}
