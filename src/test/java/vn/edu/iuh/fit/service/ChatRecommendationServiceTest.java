package vn.edu.iuh.fit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.iuh.fit.entity.Auditorium;
import vn.edu.iuh.fit.entity.Cinema;
import vn.edu.iuh.fit.entity.Genre;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Schedule;
import vn.edu.iuh.fit.entity.Showtime;
import vn.edu.iuh.fit.model.enums.MovieAge;
import vn.edu.iuh.fit.model.response.RecommendedMovieResponse;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ScheduleRepository;
import vn.edu.iuh.fit.repository.ShowtimeRepository;
import vn.edu.iuh.fit.service.chat.AgeRestrictionService;
import vn.edu.iuh.fit.service.chat.ChatCinemaLocator;
import vn.edu.iuh.fit.service.chat.ChatMemoryService;
import vn.edu.iuh.fit.service.chat.KeywordAnalyzer;
import vn.edu.iuh.fit.service.chat.KeywordAnalyzer.KeywordContext;
import vn.edu.iuh.fit.service.chat.RecommendationScoringService;
import vn.edu.iuh.fit.service.chat.RecommendationScoringService.RecommendationScoringInput;
import vn.edu.iuh.fit.service.chat.RecommendationScoringService.ScoredMovie;
import vn.edu.iuh.fit.service.chat.TextNormalizer;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ChatRecommendationServiceTest {

    @Mock
    private ChatClient chatClient;

    private ObjectMapper objectMapper;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ChatCinemaLocator cinemaLocator;

    private ChatRecommendationService chatRecommendationService;
    private KeywordAnalyzer keywordAnalyzer;
    private AgeRestrictionService ageRestrictionService;
    private RecommendationScoringService scoringService;
    private ChatMemoryService chatMemoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        keywordAnalyzer = new KeywordAnalyzer();
        ageRestrictionService = new AgeRestrictionService();
        scoringService = new RecommendationScoringService(scheduleRepository, ageRestrictionService);
        chatMemoryService = new ChatMemoryService();
        chatRecommendationService =
                new ChatRecommendationService(
                        chatClient,
                        objectMapper,
                        movieRepository,
                        showtimeRepository,
                        keywordAnalyzer,
                        scoringService,
                        ageRestrictionService,
                        chatMemoryService,
                        cinemaLocator);

        when(cinemaLocator.resolveCinema(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void standardizeGenreSlug_shouldHandleDiacriticsAndSpaces() {
        String slug = TextNormalizer.toSlug("Hài    Hước");
        assertThat(slug).isEqualTo("hai-huoc");
    }

    @Test
    void normalisePreferredGenres_shouldReturnDistinctSlugs() {
        List<String> preferred = new ArrayList<>(Arrays.asList("Hành Động", " action  ", null, ""));
        Set<String> slugs = TextNormalizer.normalizePreferredGenres(preferred);
        assertThat(slugs).isNotNull();
        assertThat(slugs).containsExactlyInAnyOrder("hanh-dong", "action");
    }

    @Test
    void extractJsonBlock_shouldFindJsonInsideCodeFence() {
        String raw = "Kết quả:\n```json\n{\"userAge\": 18, \"companionAges\": [10], \"preferredGenres\": [\"ACTION\"]}\n```";
        String json = ReflectionTestUtils.invokeMethod(chatRecommendationService, "extractJsonBlock", raw);
        assertThat(json).isEqualTo("{\"userAge\": 18, \"companionAges\": [10], \"preferredGenres\": [\"ACTION\"]}");
    }

    @Test
    void extractJsonBlock_shouldReturnEmptyObjectWhenNotFound() {
        String json = ReflectionTestUtils.invokeMethod(chatRecommendationService, "extractJsonBlock", "Không có JSON");
        assertThat(json).isEqualTo("{}");
    }

    @Test
    void calculateAge_shouldSupportSqlDate() {
        Date dob = Date.valueOf(LocalDate.now().minusYears(20));
        Integer age = ReflectionTestUtils.invokeMethod(chatRecommendationService, "calculateAge", dob);
        assertThat(age).isEqualTo(20);
    }

    @Test
    @SuppressWarnings("unchecked")
    void analyzeKeywordContext_shouldInferDinosaurPreferences() {
        KeywordContext context = keywordAnalyzer.analyze("Cho tôi phim khủng long hay nhất");
        Set<String> genreSlugs = context.genreSlugs();
        Set<String> namePatterns = context.namePatterns();

        assertThat(genreSlugs).contains("khoa-hoc", "hanh-dong");
        assertThat(namePatterns).contains("khung long");
    }

    @Test
    void extractRequestedDates_shouldParseNaturalLanguageAndDate() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate today = now.toLocalDate();
        String message = "Tư vấn phim tối nay hoặc 2025-12-24 giúp mình";

        @SuppressWarnings("unchecked")
        Set<LocalDate> dates =
            ReflectionTestUtils.invokeMethod(
                scoringService, "extractRequestedDates", message, now);

        assertThat(dates).contains(today, LocalDate.of(2025, 12, 24));
    }

    @Test
    void isAgeAllowed_shouldRejectOverAgeMovies() {
        Movie movie = Movie.builder().age(MovieAge.T18).build();
        boolean allowed = ageRestrictionService.isAgeAllowed(movie, 13);
        assertThat(allowed).isFalse();
    }

    @Test
    void scoreMovie_shouldBoostWithUpcomingSchedule() {
        Movie movie =
            Movie.builder()
                .id(1)
                .name("Jurassic Planet")
                .slug("jurassic-planet")
                .age(MovieAge.P)
                .rating(8.0)
                .publishedAt(new java.util.Date())
                .genres(
                    new LinkedHashSet<>(Set.of(Genre.builder().slug("comedy").name("Hài").build())))
                .build();

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Schedule schedule =
            Schedule.builder()
                .movie(movie)
                .startDate(java.util.Date.from(now.plusHours(2).toInstant()))
                .endDate(java.util.Date.from(now.plusHours(4).toInstant()))
                .build();

        when(scheduleRepository.findByMovie_StatusAndEndDateAfter(eq(true), any(java.util.Date.class)))
            .thenReturn(List.of(schedule));

        RecommendationScoringInput input =
            new RecommendationScoringInput(
                18,
                Set.of("comedy"),
                Set.of("jurassic"),
                "xem phim hôm nay lúc 19h",
                ZoneId.of("Asia/Ho_Chi_Minh"),
                Collections.emptySet(),
                Collections.emptySet());

        List<ScoredMovie> scored = scoringService.scoreMovies(List.of(movie), input);
        Object breakdown = ReflectionTestUtils.invokeMethod(scored.get(0), "breakdown");

        Double scheduleScore = ReflectionTestUtils.invokeMethod(breakdown, "scheduleScore");
        Double finalScore = ReflectionTestUtils.invokeMethod(breakdown, "finalScore");

        assertThat(scheduleScore).isNotNull().isGreaterThan(0.0);
        assertThat(finalScore).isNotNull().isGreaterThan(5.0);
    }

    @Test
    void isLowQualityAnswer_shouldTriggerFallbackForApology() {
        RecommendedMovieResponse movie = RecommendedMovieResponse.builder()
                .name("Titanic")
                .ageRating(null)
                .build();

        boolean lowQuality = ReflectionTestUtils.invokeMethod(
                chatRecommendationService,
                "isLowQualityAnswer",
                "Xin lỗi, tôi không thể giúp bạn với câu hỏi này",
                List.of(movie)
        );

        boolean acceptable = ReflectionTestUtils.invokeMethod(
                chatRecommendationService,
                "isLowQualityAnswer",
                "Bạn có thể thử phim Titanic, rất phù hợp",
                List.of(movie)
        );

        assertThat(lowQuality).isTrue();
        assertThat(acceptable).isFalse();
    }

    @Test
    void findRelevantShowtimes_shouldRespectDateAndCinemaFilter() {
        Movie movie = Movie.builder().id(42).build();
        Cinema cinema = Cinema.builder().id(7).name("CGV Crescent Mall").build();
        Auditorium auditorium =
            Auditorium.builder().id(11).name("Phòng 1").cinema(cinema).build();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Showtime matching =
            Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .date(today.plusDays(1))
                .startTime("19:30")
                .build();

        Showtime otherDate =
            Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .date(today.plusDays(3))
                .startTime("10:00")
                .build();

        Showtime otherCinema =
            Showtime.builder()
                .movie(movie)
                .auditorium(
                    Auditorium.builder()
                        .id(12)
                        .name("Phòng 2")
                        .cinema(Cinema.builder().id(8).name("Beta Quận 1").build())
                        .build())
                .date(today.plusDays(1))
                .startTime("20:00")
                .build();

        when(showtimeRepository.findByMovie_IdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                eq(movie.getId()), any(LocalDate.class)))
            .thenReturn(List.of(matching, otherDate, otherCinema));

        ZonedDateTime reference = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        @SuppressWarnings("unchecked")
        List<Showtime> result =
            ReflectionTestUtils.invokeMethod(
                chatRecommendationService,
                "findRelevantShowtimes",
                movie,
                Set.of(today.plusDays(1)),
                Set.of(19),
                cinema.getId(),
                reference);

        assertThat(result).containsExactly(matching);
    }
}
