package vn.edu.iuh.fit.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Schedule;
import vn.edu.iuh.fit.entity.Showtime;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertScheduleRequest;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ScheduleRepository;
import vn.edu.iuh.fit.repository.ShowtimeRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {
  private final ScheduleRepository scheduleRepository;
  private final MovieRepository movieRepository;
  private final ShowtimeRepository showtimeRepository;

  public List<Schedule> getAllSchedules() {
    return scheduleRepository.findAll(Sort.by("startDate").descending());
  }

  public Schedule saveSchedule(UpsertScheduleRequest request) {
    Movie movie =
        movieRepository
            .findById(request.getMovieId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phim với id: " + request.getMovieId()));

    LocalDate startDate = toLocalDate(request.getStartDate());
    LocalDate endDate = toLocalDate(request.getEndDate());
    validateDateRange(startDate, endDate);

    // Tìm kiếm xem movie có lịch chiếu nào đang chiếu hoặc sắp chiếu không
    // Nếu movie đang chiếu hoặc sắp chiếu thì không thể tạo lịch chiếu mới
    List<Schedule> schedules =
        scheduleRepository.findByMovieIdAndEndDateAfter(
            request.getMovieId(), request.getStartDate());
    if (!schedules.isEmpty()) {
      throw new ResourceNotFoundException(
          "Phim đang chiếu hoặc sắp chiếu không thể tạo lịch chiếu mới");
    }

    // Tạo lịch chiếu mới
    Schedule schedule =
        Schedule.builder()
            .movie(movie)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .build();
    return scheduleRepository.save(schedule);
  }

  public void deleteSchedule(Integer id) {
    // Kiểm tra xem lịch chiếu có tồn tại không
    Schedule schedule =
        scheduleRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy lịch chiếu với id: " + id));

    // Kiểm tra xem lịch chiếu đang có suất chiếu nào không
    Movie movie = schedule.getMovie();
    if (showtimeRepository.existsByMovie_Id(movie.getId())) {
      throw new ResourceNotFoundException("Không thể xóa lịch chiếu đang có suất chiếu");
    }

    // Xóa lịch chiếu
    scheduleRepository.delete(schedule);
  }

  public Schedule updateSchedule(Integer id, UpsertScheduleRequest request) {
    Schedule schedule =
        scheduleRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy lịch chiếu với id: " + id));

    Movie movie =
        movieRepository
            .findById(request.getMovieId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phim với id: " + request.getMovieId()));

    LocalDate startDate = toLocalDate(request.getStartDate());
    LocalDate endDate = toLocalDate(request.getEndDate());
    validateDateRange(startDate, endDate);

    // Không cho phép đổi phim nếu phim hiện tại đã có suất chiếu
    boolean isChangingMovie = !schedule.getMovie().getId().equals(movie.getId());
    if (isChangingMovie && showtimeRepository.existsByMovie_Id(schedule.getMovie().getId())) {
      throw new BadRequestException("Không thể đổi phim vì lịch chiếu đã có suất chiếu");
    }

    // Đảm bảo khoảng ngày mới bao phủ tất cả suất chiếu hiện có của phim
    enforceShowtimeWindow(movie.getId(), startDate, endDate);

    schedule.setMovie(movie);
    schedule.setStartDate(request.getStartDate());
    schedule.setEndDate(request.getEndDate());
    return scheduleRepository.save(schedule);
  }

  private void validateDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      return;
    }

    if (!endDate.isAfter(startDate)) {
      throw new BadRequestException("Ngày kết thúc phải lớn hơn ngày bắt đầu");
    }
  }

  private LocalDate toLocalDate(java.util.Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private void enforceShowtimeWindow(Integer movieId, LocalDate startDate, LocalDate endDate) {
    List<Showtime> showtimes = showtimeRepository.findByMovie_Id(movieId);
    if (showtimes.isEmpty()) {
      return;
    }

    LocalDate earliest =
        showtimes.stream().map(Showtime::getDate).min(LocalDate::compareTo).orElse(null);
    LocalDate latest =
        showtimes.stream().map(Showtime::getDate).max(LocalDate::compareTo).orElse(null);

    if (earliest == null || latest == null) {
      return;
    }

    if (startDate.isAfter(earliest) || endDate.isBefore(latest)) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      throw new BadRequestException(
          String.format(
              "Không thể cập nhật lịch chiếu. Phim đã có suất chiếu từ %s đến %s. "
                  + "Khoảng thời gian mới phải bao phủ các suất chiếu hiện có.",
              earliest.format(formatter), latest.format(formatter)));
    }
  }
}
