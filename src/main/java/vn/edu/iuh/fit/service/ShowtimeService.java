package vn.edu.iuh.fit.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.BulkShowtimeConflictException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.exception.SlotConflictException;
import vn.edu.iuh.fit.model.dto.MovieWithShowtimesDto;
import vn.edu.iuh.fit.model.dto.ShowtimeDto;
import vn.edu.iuh.fit.model.enums.ConflictPolicy;
import vn.edu.iuh.fit.model.enums.GraphicsType;
import vn.edu.iuh.fit.model.enums.TranslationType;
import vn.edu.iuh.fit.model.request.BulkShowtimeRequest;
import vn.edu.iuh.fit.model.request.UpsertShowtimeRequest;
import vn.edu.iuh.fit.model.response.BulkShowtimeResponse;
import vn.edu.iuh.fit.model.response.ShowtimeDetailResponse;
import vn.edu.iuh.fit.model.response.ShowtimeResponse;
import vn.edu.iuh.fit.repository.*;
import vn.edu.iuh.fit.specification.ShowtimeSpecification;

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

  public List<ShowtimeResponse> getAllShowtimes(
      Integer cinemaId, Integer auditoriumId, String showDate) {
    List<ShowtimeResponse> responses = new ArrayList<>();

    if (cinemaId != null) {
      // Process a specific cinema
      Cinema cinema =
          cinemaRepository
              .findById(cinemaId)
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

  private void addCinemaToShowtimeResponse(
      Cinema cinema, Integer auditoriumId, String showDate, List<ShowtimeResponse> responses) {
    List<ShowtimeResponse.AuditoriumResponse> auditoriumResponses = new ArrayList<>();

    List<Auditorium> auditoriums =
        auditoriumId != null
            ? auditoriumRepository.findById(auditoriumId).map(List::of).orElse(new ArrayList<>())
            : auditoriumRepository.findByCinemaId(cinema.getId());

    for (Auditorium auditorium : auditoriums) {
      Specification<Showtime> specification =
          ShowtimeSpecification.filterShowtimes(cinema.getId(), auditorium.getId(), showDate);
      List<Showtime> showTimes =
          showtimeRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "startTime"));

      auditoriumResponses.add(
          ShowtimeResponse.AuditoriumResponse.builder()
              .auditorium(auditorium)
              .showtimes(showTimes)
              .build());
    }

    // This ensures that cinemas with no auditoriums are also included in the response with an empty
    // list of auditoriums
    responses.add(
        ShowtimeResponse.builder().cinema(cinema).auditoriums(auditoriumResponses).build());
  }

  public Showtime createShowtimes(UpsertShowtimeRequest request) {
    log.info("Creating showtime: {}", request);
    Auditorium auditorium =
        auditoriumRepository
            .findById(request.getAuditoriumId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phòng chiếu có id = " + request.getAuditoriumId()));

    Movie movie =
        movieRepository
            .findById(request.getMovieId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phim có id = " + request.getMovieId()));

    // === SLOT VALIDATION ===
    // Validate slot-based showtime (start/end time, movie runtime, spans)
    slotValidationService.validateSlotBasedShowtime(
        request.getStartTime(), request.getEndTime(), movie.getDuration());

    log.info(
        "Slot validation passed for movie '{}' ({}min) occupying {}",
        movie.getName(),
        movie.getDuration(),
        slotValidationService.getOccupiedSlotsDescription(
            request.getStartTime(), request.getEndTime()));

    // Kiểm tra xem movie có lịch chiếu trong ngày đã chọn chưa
    List<Schedule> schedules = scheduleRepository.findByMovie_Id(movie.getId());
    if (schedules.isEmpty()) {
      throw new BadRequestException("Phim chưa có lịch chiếu");
    }

    // Kiểm tra xem lịch chiếu đã hết hạn hay chưa dựa vào endDate của schedule với date trong
    // request
    // Lặp qua từng schedule để kiểm tra
    for (Schedule schedule : schedules) {
      // Convert date từ request sang Date để so sánh với endDate của schedule
      Date dateRequest =
          Date.from(request.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
      if (schedule.getEndDate().before(dateRequest)) {
        throw new BadRequestException("Lịch chiếu đã hết hạn");
      }
    }

    // === SLOT CONFLICT DETECTION ===
    // Kiểm tra xem ngày giờ trong request có nằm trong khoảng thời gian của showtime khác không
    List<Showtime> existingShowtimes =
        showtimeRepository.findByAuditorium_IdAndDate(auditorium.getId(), request.getDate());
    for (Showtime existingShowtime : existingShowtimes) {
      if (slotValidationService.isTimeOverlap(
          request.getStartTime(),
          request.getEndTime(),
          existingShowtime.getStartTime(),
          existingShowtime.getEndTime())) {
        String conflictSlots =
            slotValidationService.getOccupiedSlotsDescription(
                existingShowtime.getStartTime(), existingShowtime.getEndTime());
        throw new SlotConflictException(
            "Xung đột lịch chiếu! "
                + conflictSlots
                + " đã được phim '"
                + existingShowtime.getMovie().getName()
                + "' sử dụng");
      }
    }

    Showtime showtime =
        Showtime.builder()
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
    Movie movie =
        movieRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim có id = " + id));
    // Kiểm tra xem phim có lịch chiếu nào không trong 20 ngày. Tính từ ngày hiện tại trở về sau
    LocalDate currentDate = LocalDate.now(ZoneId.systemDefault());
    LocalDate endDate = currentDate.plusDays(20);

    boolean hasShowtimes =
        showtimeRepository.existsByMovie_IdAndDateBetween(id, currentDate, endDate);

    return Map.of("hasShowtimes", hasShowtimes);
  }

  public Movie getMovieByShowtimeId(Integer showtimeId) {
    return showtimeRepository.findById(showtimeId).map(Showtime::getMovie).orElse(null);
  }

  public ShowtimeDetailResponse getShowtimeDetail(Integer id) {
    Showtime showtime =
        showtimeRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));

    Movie movie = showtime.getMovie();
    Auditorium auditorium = showtime.getAuditorium();
    Cinema cinema = auditorium != null ? auditorium.getCinema() : null;

    return ShowtimeDetailResponse.builder()
        .id(showtime.getId())
        .date(showtime.getDate())
        .startTime(showtime.getStartTime())
        .endTime(showtime.getEndTime())
        .graphicsType(showtime.getGraphicsType() != null ? showtime.getGraphicsType().name() : null)
        .translationType(
            showtime.getTranslationType() != null ? showtime.getTranslationType().name() : null)
        .cinemaId(cinema != null ? cinema.getId() : null)
        .cinemaName(cinema != null ? cinema.getName() : null)
        .cinemaAddress(cinema != null ? cinema.getAddress() : null)
        .auditoriumId(auditorium != null ? auditorium.getId() : null)
        .auditoriumName(auditorium != null ? auditorium.getName() : null)
        .auditoriumTotalSeats(auditorium != null ? auditorium.getTotalSeats() : null)
        .auditoriumTotalRows(auditorium != null ? auditorium.getTotalRows() : null)
        .auditoriumTotalColumns(auditorium != null ? auditorium.getTotalColumns() : null)
        .auditoriumType(
            auditorium != null && auditorium.getType() != null ? auditorium.getType().name() : null)
        .movieId(movie != null ? movie.getId() : null)
        .movieName(movie != null ? movie.getName() : null)
        .movieSlug(movie != null ? movie.getSlug() : null)
        .moviePoster(movie != null ? movie.getPoster() : null)
        .movieAge(movie != null ? movie.getAge() : null)
        .movieRating(movie != null ? movie.getRating() : null)
        .movieDuration(movie != null ? movie.getDuration() : null)
        .build();
  }

  public List<MovieWithShowtimesDto> getShowtimesByCinema(Integer cinemaId) {
    List<Auditorium> auditoriums = auditoriumRepository.findByCinema_Id(cinemaId);

    LocalDate today = LocalDate.now();
    LocalDate endDate = today.plusDays(10);

    // Lấy danh sách phim đang chiếu
    List<Movie> showingNowMovies =
        scheduleRepository
            .findByMovie_StatusAndStartDateBeforeAndEndDateAfter(true, new Date(), new Date())
            .stream()
            .map(Schedule::getMovie)
            .toList();

    Set<Integer> showingMovieIds =
        showingNowMovies.stream().map(Movie::getId).collect(Collectors.toSet());

    Map<Integer, MovieWithShowtimesDto> movieMap = new LinkedHashMap<>();

    for (Auditorium auditorium : auditoriums) {
      List<Showtime> showtimes = showtimeRepository.findByAuditorium_Id(auditorium.getId());

      showtimes.stream()
          .filter(s -> !s.getDate().isBefore(today) && !s.getDate().isAfter(endDate))
          .filter(s -> showingMovieIds.contains(s.getMovie().getId()))
          .forEach(
              s -> {
                Movie m = s.getMovie();

                movieMap.computeIfAbsent(
                    m.getId(),
                    k -> {
                      MovieWithShowtimesDto dto = new MovieWithShowtimesDto();
                      dto.setId(m.getId());
                      dto.setName(m.getName());
                      dto.setNameEn(m.getNameEn());
                      dto.setDescription(m.getDescription());
                      dto.setDuration(m.getDuration());
                      dto.setPoster(m.getPoster());
                      dto.setRating(m.getRating());
                      dto.setReleaseYear(m.getReleaseYear());
                      dto.setAge(m.getAge() != null ? m.getAge().name() : null);
                      dto.setTrailer(m.getTrailer());
                      dto.setStatus(m.getStatus() != null ? m.getStatus() : false);
                      dto.setSlug(m.getSlug());
                      dto.setCreatedAt(m.getCreatedAt());
                      dto.setUpdatedAt(m.getUpdatedAt());
                      dto.setGraphics(m.getGraphics().toString());
                      dto.setTranslations(m.getTranslations().toString());
                      return dto;
                    });

                ShowtimeDto showtimeDto =
                    new ShowtimeDto(
                        s.getId(),
                        s.getDate(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getGraphicsType() != null ? s.getGraphicsType().name() : null,
                        s.getTranslationType() != null ? s.getTranslationType().name() : null,
                        auditorium.getCinema().getId(),
                        auditorium.getCinema().getName(),
                        auditorium.getCinema().getMapLocation(),
                        auditorium.getId(),
                        auditorium.getName(),
                        auditorium.getTotalSeats(),
                        auditorium.getTotalRows(),
                        auditorium.getTotalColumns(),
                        auditorium.getType() != null ? auditorium.getType().name() : null,
                        s.getCreatedAt(),
                        s.getUpdatedAt());

                movieMap.get(m.getId()).getShowtimes().add(showtimeDto);
              });
    }

    return new ArrayList<>(movieMap.values());
  }

  public List<MovieWithShowtimesDto> getShowtimesByCinemaName(String cinemaName) {
    // Tìm cinema theo tên
    Cinema cinema =
        cinemaRepository
            .findByNameIgnoreCase(cinemaName)
            .orElseThrow(() -> new RuntimeException("Cinema not found with name: " + cinemaName));

    // Lấy danh sách auditorium của cinema đó
    List<Auditorium> auditoriums = auditoriumRepository.findByCinema_Id(cinema.getId());

    LocalDate today = LocalDate.now();
    LocalDate endDate = today.plusDays(10);

    // Lấy danh sách phim đang chiếu
    List<Movie> showingNowMovies =
        scheduleRepository
            .findByMovie_StatusAndStartDateBeforeAndEndDateAfter(true, new Date(), new Date())
            .stream()
            .map(Schedule::getMovie)
            .toList();

    Set<Integer> showingMovieIds =
        showingNowMovies.stream().map(Movie::getId).collect(Collectors.toSet());

    Map<Integer, MovieWithShowtimesDto> movieMap = new LinkedHashMap<>();

    for (Auditorium auditorium : auditoriums) {
      List<Showtime> showtimes = showtimeRepository.findByAuditorium_Id(auditorium.getId());

      showtimes.stream()
          .filter(s -> !s.getDate().isBefore(today) && !s.getDate().isAfter(endDate))
          .filter(s -> showingMovieIds.contains(s.getMovie().getId()))
          .forEach(
              s -> {
                Movie m = s.getMovie();

                movieMap.computeIfAbsent(
                    m.getId(),
                    k -> {
                      MovieWithShowtimesDto dto = new MovieWithShowtimesDto();
                      dto.setId(m.getId());
                      dto.setName(m.getName());
                      dto.setNameEn(m.getNameEn());
                      dto.setDescription(m.getDescription());
                      dto.setDuration(m.getDuration());
                      dto.setPoster(m.getPoster());
                      dto.setRating(m.getRating());
                      dto.setReleaseYear(m.getReleaseYear());
                      dto.setAge(m.getAge() != null ? m.getAge().name() : null);
                      dto.setTrailer(m.getTrailer());
                      dto.setStatus(m.getStatus() != null ? m.getStatus() : false);
                      dto.setSlug(m.getSlug());
                      dto.setCreatedAt(m.getCreatedAt());
                      dto.setUpdatedAt(m.getUpdatedAt());
                      dto.setGraphics(m.getGraphics().toString());
                      dto.setTranslations(m.getTranslations().toString());
                      return dto;
                    });

                ShowtimeDto showtimeDto =
                    new ShowtimeDto(
                        s.getId(),
                        s.getDate(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getGraphicsType() != null ? s.getGraphicsType().name() : null,
                        s.getTranslationType() != null ? s.getTranslationType().name() : null,
                        auditorium.getCinema().getId(),
                        auditorium.getCinema().getName(),
                        auditorium.getCinema().getMapLocation(),
                        auditorium.getId(),
                        auditorium.getName(),
                        auditorium.getTotalSeats(),
                        auditorium.getTotalRows(),
                        auditorium.getTotalColumns(),
                        auditorium.getType() != null ? auditorium.getType().name() : null,
                        s.getCreatedAt(),
                        s.getUpdatedAt());

                movieMap.get(m.getId()).getShowtimes().add(showtimeDto);
              });
    }

    return new ArrayList<>(movieMap.values());
  }

  /** Tạo nhiều suất chiếu theo khoảng ngày với xử lý conflict */
  public BulkShowtimeResponse createBulkShowtimes(BulkShowtimeRequest request) {
    log.info("Creating bulk showtimes: {}", request);

    // Validate basic data
    Auditorium auditorium =
        auditoriumRepository
            .findById(request.getAuditoriumId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phòng chiếu có id = " + request.getAuditoriumId()));

    Movie movie =
        movieRepository
            .findById(request.getMovieId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy phim có id = " + request.getMovieId()));

    // Validate date range
    if (request.getDateFrom().isAfter(request.getDateTo())) {
      throw new BadRequestException("Ngày bắt đầu không thể sau ngày kết thúc");
    }

    // Validate date range not too large (max 90 days)
    if (request.getDateFrom().plusDays(90).isBefore(request.getDateTo())) {
      throw new BadRequestException("Khoảng ngày tối đa là 90 ngày");
    }

    // Validate slot-based showtime
    slotValidationService.validateSlotBasedShowtime(
        request.getStartTime(), request.getEndTime(), movie.getDuration());

    // Generate target dates based on date range and days of week
    List<LocalDate> targetDates =
        generateTargetDates(request.getDateFrom(), request.getDateTo(), request.getDaysOfWeek());

    // Check for conflicts
    List<BulkShowtimeResponse.ConflictDetail> conflicts = new ArrayList<>();
    List<LocalDate> validDates = new ArrayList<>();

    for (LocalDate date : targetDates) {
      BulkShowtimeResponse.ConflictDetail conflict =
          checkDateConflict(
              auditorium.getId(), date, request.getStartTime(), request.getEndTime(), movie);

      if (conflict != null) {
        conflicts.add(conflict);
      } else {
        validDates.add(date);
      }
    }

    // Handle conflicts based on policy
    if (!conflicts.isEmpty() && request.getConflictPolicy() == ConflictPolicy.FAIL) {
      BulkShowtimeResponse response =
          BulkShowtimeResponse.builder()
              .totalRequested(targetDates.size())
              .successfullyCreated(0)
              .conflictsDetected(conflicts.size())
              .conflicts(conflicts)
              .message(
                  "Phát hiện xung đột lịch chiếu. Vui lòng chọn chính sách bỏ qua hoặc chọn ngày khác.")
              .build();

      throw new BulkShowtimeConflictException(
          "Bulk showtime creation failed due to conflicts", response);
    }

    // Create showtimes for valid dates
    List<Showtime> createdShowtimes = new ArrayList<>();
    for (LocalDate date : validDates) {
      try {
        Showtime showtime =
            createSingleShowtime(
                movie,
                auditorium,
                date,
                request.getStartTime(),
                request.getEndTime(),
                request.getGraphicsType(),
                request.getTranslationType());
        createdShowtimes.add(showtime);
      } catch (Exception e) {
        log.error("Failed to create showtime for date {}: {}", date, e.getMessage());
        // Add to conflicts if individual creation fails
        conflicts.add(
            BulkShowtimeResponse.ConflictDetail.builder()
                .date(date)
                .reason("Lỗi tạo suất chiếu: " + e.getMessage())
                .build());
      }
    }

    return BulkShowtimeResponse.builder()
        .totalRequested(targetDates.size())
        .successfullyCreated(createdShowtimes.size())
        .conflictsDetected(conflicts.size())
        .skipped(conflicts.size())
        .createdShowtimes(createdShowtimes)
        .conflicts(conflicts)
        .skippedDates(conflicts.stream().map(BulkShowtimeResponse.ConflictDetail::getDate).toList())
        .message(
            String.format(
                "Đã tạo %d/%d suất chiếu thành công", createdShowtimes.size(), targetDates.size()))
        .build();
  }

  /** Sinh danh sách ngày dựa trên khoảng thời gian và các ngày trong tuần */
  private List<LocalDate> generateTargetDates(
      LocalDate dateFrom, LocalDate dateTo, List<Integer> daysOfWeek) {
    List<LocalDate> dates = new ArrayList<>();
    LocalDate current = dateFrom;

    while (!current.isAfter(dateTo)) {
      // Java DayOfWeek: 1=Monday, 7=Sunday
      // Request format: 1=Sunday, 2=Monday, ..., 7=Saturday
      int javaDayOfWeek = current.getDayOfWeek().getValue(); // 1-7 (Mon-Sun)
      int requestDayOfWeek =
          javaDayOfWeek == 7 ? 1 : javaDayOfWeek + 1; // Convert to request format

      if (daysOfWeek == null || daysOfWeek.isEmpty() || daysOfWeek.contains(requestDayOfWeek)) {
        dates.add(current);
      }
      current = current.plusDays(1);
    }

    return dates;
  }

  /** Kiểm tra conflict cho một ngày cụ thể */
  private BulkShowtimeResponse.ConflictDetail checkDateConflict(
      Integer auditoriumId, LocalDate date, String startTime, String endTime, Movie movie) {

    // First check if date is within movie schedule range
    List<Schedule> schedules = scheduleRepository.findByMovie_Id(movie.getId());
    if (schedules.isEmpty()) {
      return BulkShowtimeResponse.ConflictDetail.builder()
          .date(date)
          .reason("Phim chưa có lịch chiếu")
          .build();
    }

    // Check if date is within any valid schedule
    Date dateRequest = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    boolean isDateValid = false;
    for (Schedule schedule : schedules) {
      if (!schedule.getStartDate().after(dateRequest)
          && !schedule.getEndDate().before(dateRequest)) {
        isDateValid = true;
        break;
      }
    }

    if (!isDateValid) {
      return BulkShowtimeResponse.ConflictDetail.builder()
          .date(date)
          .reason(
              "Ngày không nằm trong lịch chiếu của phim (từ "
                  + schedules.get(0).getStartDate()
                  + " đến "
                  + schedules.get(0).getEndDate()
                  + ")")
          .build();
    }

    // Then check for time conflicts with existing showtimes
    List<Showtime> existingShowtimes =
        showtimeRepository.findByAuditorium_IdAndDate(auditoriumId, date);

    for (Showtime existing : existingShowtimes) {
      if (slotValidationService.isTimeOverlap(
          startTime, endTime, existing.getStartTime(), existing.getEndTime())) {

        return BulkShowtimeResponse.ConflictDetail.builder()
            .date(date)
            .conflictMovie(existing.getMovie().getName())
            .conflictTimeRange(existing.getStartTime() + " - " + existing.getEndTime())
            .reason("Trùng với suất chiếu đã tồn tại")
            .build();
      }
    }

    return null; // No conflict
  }

  /**
   * Tạo một showtime đơn lẻ (helper method) Schedule validation should be done before calling this
   * method
   */
  private Showtime createSingleShowtime(
      Movie movie,
      Auditorium auditorium,
      LocalDate date,
      String startTime,
      String endTime,
      GraphicsType graphicsType,
      TranslationType translationType) {

    Showtime showtime =
        Showtime.builder()
            .movie(movie)
            .auditorium(auditorium)
            .graphicsType(graphicsType)
            .translationType(translationType)
            .date(date)
            .startTime(startTime)
            .endTime(endTime)
            .build();

    return showtimeRepository.save(showtime);
  }
}
