package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.exception.SlotConflictException;
import vn.edu.iuh.fit.model.request.UpsertShowtimeRequest;
import vn.edu.iuh.fit.model.response.ShowtimeResponse;
import vn.edu.iuh.fit.repository.*;
import vn.edu.iuh.fit.specification.ShowtimeSpecification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeService {
    private final ShowtimeRepository showtimeRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final MovieRepository movieRepository;
    private final ScheduleRepository scheduleRepository;
    private final SlotValidationService slotValidationService;

    public List<ShowtimeResponse> getAllShowtimes(Integer cinemaId, Integer auditoriumId, String showDate) {
        List<ShowtimeResponse> responses = new ArrayList<>();

        if (cinemaId != null) {
            // Process a specific cinema
            Cinema cinema = cinemaRepository.findById(cinemaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));
            addCinemaToShowtimeResponse(cinema, auditoriumId, showDate, responses);
        } else {
            // Process all cinemas
            List<Cinema> cinemas = cinemaRepository.findAll();
            for (Cinema cinema : cinemas) {
                addCinemaToShowtimeResponse(cinema, null, showDate, responses);
            }
        }

        return responses;
    }

    private void addCinemaToShowtimeResponse(Cinema cinema, Integer auditoriumId, String showDate, List<ShowtimeResponse> responses) {
        List<ShowtimeResponse.AuditoriumResponse> auditoriumResponses = new ArrayList<>();

        List<Auditorium> auditoriums = auditoriumId != null ?
                auditoriumRepository.findById(auditoriumId).map(List::of).orElse(new ArrayList<>()) :
                auditoriumRepository.findByCinemaId(cinema.getId());

        for (Auditorium auditorium : auditoriums) {
            Specification<Showtime> specification = ShowtimeSpecification.filterShowtimes(cinema.getId(), auditorium.getId(), showDate);
            List<Showtime> showTimes = showtimeRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "startTime"));

            auditoriumResponses.add(ShowtimeResponse.AuditoriumResponse.builder()
                    .auditorium(auditorium)
                    .showtimes(showTimes)
                    .build());
        }

        // This ensures that cinemas with no auditoriums are also included in the response with an empty list of auditoriums
        responses.add(ShowtimeResponse.builder()
                .cinema(cinema)
                .auditoriums(auditoriumResponses)
                .build());
    }

    public Showtime createShowtimes(UpsertShowtimeRequest request) {
        log.info("Creating showtime: {}", request);
        Auditorium auditorium = auditoriumRepository.findById(request.getAuditoriumId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chiếu có id = " + request.getAuditoriumId()));

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim có id = " + request.getMovieId()));

        // === SLOT VALIDATION ===
        // Validate slot-based showtime (start/end time, movie runtime, spans)
        slotValidationService.validateSlotBasedShowtime(
            request.getStartTime(), 
            request.getEndTime(), 
            movie.getDuration()
        );
        
        log.info("Slot validation passed for movie '{}' ({}min) occupying {}", 
            movie.getName(), 
            movie.getDuration(),
            slotValidationService.getOccupiedSlotsDescription(request.getStartTime(), request.getEndTime())
        );

        // Kiểm tra xem movie có lịch chiếu trong ngày đã chọn chưa
        List<Schedule> schedules = scheduleRepository.findByMovie_Id(movie.getId());
        if (schedules.isEmpty()) {
            throw new BadRequestException("Phim chưa có lịch chiếu");
        }

        // Kiểm tra xem lịch chiếu đã hết hạn hay chưa dựa vào endDate của schedule với date trong request
        // Lặp qua từng schedule để kiểm tra
        for (Schedule schedule : schedules) {
            // Convert date từ request sang Date để so sánh với endDate của schedule
            Date dateRequest = Date.from(request.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            if (schedule.getEndDate().before(dateRequest)) {
                throw new BadRequestException("Lịch chiếu đã hết hạn");
            }
        }

        // === SLOT CONFLICT DETECTION ===
        // Kiểm tra xem ngày giờ trong request có nằm trong khoảng thời gian của showtime khác không
        List<Showtime> existingShowtimes = showtimeRepository.findByAuditorium_IdAndDate(auditorium.getId(), request.getDate());
        for (Showtime existingShowtime : existingShowtimes) {
            if (slotValidationService.isTimeOverlap(request.getStartTime(), request.getEndTime(),
                    existingShowtime.getStartTime(), existingShowtime.getEndTime())) {
                String conflictSlots = slotValidationService.getOccupiedSlotsDescription(
                    existingShowtime.getStartTime(), existingShowtime.getEndTime()
                );
                throw new SlotConflictException(
                    "Xung đột lịch chiếu! " + conflictSlots + " đã được phim '" + 
                    existingShowtime.getMovie().getName() + "' sử dụng"
                );
            }
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .graphicsType(request.getGraphicsType())
                .translationType(request.getTranslationType())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        return showtimeRepository.save(showtime);
    }

    // Legacy time overlap method - now delegated to SlotValidationService
    private boolean isTimeOverlap(String start1, String end1, String start2, String end2) {
        return slotValidationService.isTimeOverlap(start1, end1, start2, end2);
    }

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
