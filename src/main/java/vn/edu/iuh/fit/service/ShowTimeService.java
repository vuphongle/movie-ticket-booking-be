package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Showtime;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ShowTimeRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowTimeService {
    private final ShowTimeRepository showtimeRepository;
    private final MovieRepository movieRepository;

    public List<Showtime> getShowtimesByMovie(Integer movieId, String showDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate showDate = LocalDate.parse(showDateStr, formatter);
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());

        List<Showtime> showtimes = showtimeRepository.findByMovie_IdAndDate(movieId, showDate);

        if (showDate.isEqual(currentDate)) {
            // Nếu showDate là ngày hiện tại, lọc ra những showtime có startTime sau thời gian hiện tại
            LocalTime currentTime = LocalTime.now(ZoneId.systemDefault());
            return showtimes.stream()
                    .filter(showtime -> LocalTime.parse(showtime.getStartTime()).isAfter(currentTime))
                    .collect(Collectors.toList());
        } else {
            // Nếu showDate là ngày trong quá khứ hoặc tương lai, trả về tất cả showtime
            return showtimes;
        }
    }

    public Map<String, Boolean> checkMovieHasShowtimes(Integer id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim có id = " + id));
        // Kiểm tra xem phim có lịch chiếu nào không trong 20 ngày. Tính từ ngày hiện tại trở về sau
        LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
        LocalDate endDate = currentDate.plusDays(20);

        boolean hasShowtimes = showtimeRepository.existsByMovie_IdAndDateBetween(id, currentDate, endDate);

        return Map.of("hasShowtimes", hasShowtimes);
    }

    public Movie getMovieByShowtimeId(Integer showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .map(Showtime::getMovie)
                .orElse(null);
    }
}
