package vn.edu.iuh.fit.service.chat;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Schedule;
import vn.edu.iuh.fit.repository.ScheduleRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScoringService {

  private static final int SCHEDULE_LOOKAHEAD_DAYS = 7;
  private static final Pattern ISO_DATE_PATTERN =
      Pattern.compile("\\b(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})\\b");
  private static final Pattern SHORT_DATE_PATTERN =
      Pattern.compile("(?<!\\d)(\\d{1,2})[-/](\\d{1,2})\\b");
  private static final Pattern HOUR_PATTERN =
      Pattern.compile("\\b(\\d{1,2})h\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLON_TIME_PATTERN = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");

  private final ScheduleRepository scheduleRepository;
  private final AgeRestrictionService ageRestrictionService;

  public List<ScoredMovie> scoreMovies(
      List<Movie> candidateMovies, RecommendationScoringInput input) {
    if (CollectionUtils.isEmpty(candidateMovies)) {
      return Collections.emptyList();
    }

    ZonedDateTime now = ZonedDateTime.now(input.zone());
    QueryContext context = extractQueryContext(input.originalMessage(), now);
    Map<Integer, List<Schedule>> upcomingSchedules = loadUpcomingSchedules(candidateMovies, now);
    String normalizedQuery = TextNormalizer.normalizeText(input.originalMessage());
    Set<LocalDate> requestedDates =
        CollectionUtils.isEmpty(input.requestedDates())
            ? context.requestedDates()
            : input.requestedDates();
    Set<Integer> requestedHours =
        CollectionUtils.isEmpty(input.requestedHours())
            ? context.requestedHours()
            : input.requestedHours();

    return candidateMovies.stream()
        .map(
            movie ->
                evaluateMovie(
                    movie,
                    input.groupMinimumAge(),
                    input.preferredGenres(),
                    input.keywordNamePatterns(),
                    normalizedQuery,
                    requestedDates,
                    requestedHours,
                    now,
                    upcomingSchedules.getOrDefault(movie.getId(), Collections.emptyList())))
        .filter(Objects::nonNull)
        .toList();
  }

  public QueryContext extractQueryContext(String message, ZoneId zone) {
    ZoneId effectiveZone = zone == null ? ZoneId.systemDefault() : zone;
    return extractQueryContext(message, ZonedDateTime.now(effectiveZone));
  }

  public QueryContext extractQueryContext(String message, ZonedDateTime referenceTime) {
    ZonedDateTime effectiveReference =
        referenceTime == null ? ZonedDateTime.now(ZoneId.systemDefault()) : referenceTime;
    Set<LocalDate> dates = extractRequestedDates(message, effectiveReference);
    Set<Integer> hours = extractRequestedHours(message);
    return new QueryContext(dates, hours);
  }

  public void logTopCandidates(List<ScoredMovie> scoredMovies) {
    if (!log.isInfoEnabled() || CollectionUtils.isEmpty(scoredMovies)) {
      return;
    }
    scoredMovies.stream()
        .sorted(
            Comparator.comparingDouble((ScoredMovie scored) -> scored.breakdown().finalScore())
                .reversed())
        .limit(20)
        .forEach(
            scored ->
                log.info(
                    "[ChatRecommend] movie='{}' ageFail={} genreScore={} nameScore={} scheduleBoost={} ratingScore={} freshness={} finalScore={}",
                    scored.movie().getName(),
                    scored.breakdown().ageAllowed() ? 0 : 1,
                    formatScore(scored.breakdown().genreScore()),
                    formatScore(scored.breakdown().nameScore()),
                    formatScore(scored.breakdown().scheduleScore()),
                    formatScore(scored.breakdown().ratingScore()),
                    formatScore(scored.breakdown().freshnessScore()),
                    formatScore(scored.breakdown().finalScore())));
  }

  private ScoredMovie evaluateMovie(
      Movie movie,
      int groupMinAge,
      Set<String> preferredGenres,
      Set<String> keywordPatterns,
      String normalizedQuery,
      Set<LocalDate> requestedDates,
      Set<Integer> requestedHours,
      ZonedDateTime now,
      List<Schedule> schedules) {
    if (movie == null || movie.getId() == null) {
      return null;
    }

    boolean ageAllowed = ageRestrictionService.isAgeAllowed(movie, groupMinAge);
    double ageScore = ageAllowed ? 5.0 : -50.0;

    Set<String> movieGenres = extractMovieGenreSlugs(movie);
    double genreScore = computeGenreScore(movieGenres, preferredGenres);
    double nameScore = computeNameScore(movie, keywordPatterns, normalizedQuery);
    double ratingScore = computeRatingScore(movie);
    double freshnessScore = computeFreshnessScore(movie, now);
    double scheduleScore =
        computeScheduleScore(movie, schedules, requestedDates, requestedHours, now);

    double finalScore =
        ageScore + genreScore + nameScore + ratingScore + freshnessScore + scheduleScore;

    ScoreBreakdown breakdown =
        new ScoreBreakdown(
            ageAllowed,
            ageScore,
            genreScore,
            nameScore,
            ratingScore,
            freshnessScore,
            scheduleScore,
            finalScore);

    return new ScoredMovie(movie, breakdown);
  }

  private Set<String> extractMovieGenreSlugs(Movie movie) {
    if (movie == null || CollectionUtils.isEmpty(movie.getGenres())) {
      return Collections.emptySet();
    }
    return movie.getGenres().stream()
        .map(genre -> StringUtils.hasText(genre.getSlug()) ? genre.getSlug() : genre.getName())
        .filter(StringUtils::hasText)
        .map(TextNormalizer::toSlug)
        .filter(StringUtils::hasText)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private double computeGenreScore(Set<String> movieGenres, Set<String> preferredGenres) {
    if (CollectionUtils.isEmpty(movieGenres) || CollectionUtils.isEmpty(preferredGenres)) {
      return 0.0;
    }
    long matches = movieGenres.stream().filter(preferredGenres::contains).count();
    if (matches == 0) {
      return 0.0;
    }
    double denominator = Math.max(1.0, Math.min(preferredGenres.size(), movieGenres.size()));
    double ratio = Math.min(1.0, matches / denominator);
    return ratio * 4.0;
  }

  private double computeNameScore(
      Movie movie, Set<String> keywordPatterns, String normalizedQuery) {
    double score = 0.0;
    String normalizedName = TextNormalizer.normalizeText(movie.getName());
    String normalizedSlug = TextNormalizer.normalizeText(movie.getSlug());

    if (!CollectionUtils.isEmpty(keywordPatterns)) {
      score = Math.max(score, calculatePatternScore(keywordPatterns, normalizedName));
      score = Math.max(score, calculatePatternScore(keywordPatterns, normalizedSlug));
    }

    if (StringUtils.hasText(normalizedQuery)) {
      score = Math.max(score, calculateQueryMatchScore(normalizedQuery, normalizedName));
      score = Math.max(score, calculateQueryMatchScore(normalizedQuery, normalizedSlug));
    }
    return score;
  }

  private double calculatePatternScore(Set<String> patterns, String target) {
    if (CollectionUtils.isEmpty(patterns) || !StringUtils.hasText(target)) {
      return 0.0;
    }
    return patterns.stream().mapToDouble(pattern -> scoreMatch(pattern, target)).max().orElse(0.0);
  }

  private double scoreMatch(String pattern, String target) {
    if (!StringUtils.hasText(pattern) || !StringUtils.hasText(target)) {
      return 0.0;
    }
    if (pattern.equals(target)) {
      return 4.0;
    }
    if (target.startsWith(pattern)) {
      return 2.5;
    }
    if (target.contains(pattern)) {
      double coverage = (double) pattern.length() / Math.max(1, target.length());
      return 1.2 + coverage;
    }
    return 0.0;
  }

  private double calculateQueryMatchScore(String query, String target) {
    if (!StringUtils.hasText(query) || !StringUtils.hasText(target)) {
      return 0.0;
    }
    if (query.equals(target)) {
      return 4.0;
    }
    if (query.contains(target)) {
      return target.length() >= 6 ? 3.0 : 2.0;
    }
    List<String> tokens = List.of(target.split(" "));
    long tokenMatches =
        tokens.stream().filter(token -> token.length() >= 3).filter(query::contains).count();
    if (tokenMatches == 0) {
      return 0.0;
    }
    return Math.min(2.0, tokenMatches * 0.6);
  }

  private double computeRatingScore(Movie movie) {
    if (movie.getRating() == null) {
      return 0.0;
    }
    double rating = Math.max(0.0, Math.min(10.0, movie.getRating()));
    return (rating / 10.0) * 4.0;
  }

  private double computeFreshnessScore(Movie movie, ZonedDateTime now) {
    java.util.Date reference =
        movie.getPublishedAt() != null ? movie.getPublishedAt() : movie.getCreatedAt();
    if (reference == null) {
      return 0.0;
    }
    ZonedDateTime referenceDate = ZonedDateTime.ofInstant(reference.toInstant(), now.getZone());
    long daysSince =
        java.time.temporal.ChronoUnit.DAYS.between(referenceDate.toLocalDate(), now.toLocalDate());
    if (daysSince <= 7) {
      return 1.5;
    }
    if (daysSince <= 30) {
      return 1.0;
    }
    if (daysSince <= 90) {
      return 0.5;
    }
    return 0.0;
  }

  private double computeScheduleScore(
      Movie movie,
      List<Schedule> schedules,
      Set<LocalDate> requestedDates,
      Set<Integer> requestedHours,
      ZonedDateTime now) {
    if (CollectionUtils.isEmpty(schedules)) {
      return 0.0;
    }
    ZonedDateTime soonestStart = null;
    boolean matchesRequestedDate = false;
    boolean matchesRequestedHour = false;

    for (Schedule schedule : schedules) {
      if (schedule.getStartDate() == null) {
        continue;
      }
      ZonedDateTime start =
          ZonedDateTime.ofInstant(schedule.getStartDate().toInstant(), now.getZone());
      if (start.isBefore(now.minusHours(1))) {
        continue;
      }
      if (soonestStart == null || start.isBefore(soonestStart)) {
        soonestStart = start;
      }
      LocalDate startDate = start.toLocalDate();
      if (!CollectionUtils.isEmpty(requestedDates) && requestedDates.contains(startDate)) {
        matchesRequestedDate = true;
        if (!CollectionUtils.isEmpty(requestedHours)) {
          int hour = start.getHour();
          matchesRequestedHour =
              requestedHours.stream().anyMatch(requestHour -> Math.abs(requestHour - hour) <= 1);
        }
      }
    }

    if (soonestStart == null) {
      return 0.0;
    }

    long minutesUntil = Math.max(0, Duration.between(now, soonestStart).toMinutes());
    double daysUntil = minutesUntil / 1440.0;
    double score;
    if (daysUntil <= 0.5) {
      score = 3.5;
    } else if (daysUntil <= 1) {
      score = 3.0;
    } else if (daysUntil <= 3) {
      score = 2.2;
    } else if (daysUntil <= SCHEDULE_LOOKAHEAD_DAYS) {
      score = 1.5;
    } else if (daysUntil <= 14) {
      score = 0.7;
    } else {
      score = 0.0;
    }

    if (matchesRequestedDate) {
      score += matchesRequestedHour ? 1.5 : 0.8;
    }

    return score;
  }

  private Map<Integer, List<Schedule>> loadUpcomingSchedules(
      List<Movie> candidateMovies, ZonedDateTime now) {
    Set<Integer> movieIds =
        candidateMovies.stream()
            .map(Movie::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (movieIds.isEmpty()) {
      return Collections.emptyMap();
    }

    java.util.Date threshold = java.util.Date.from(now.toInstant());
    List<Schedule> upcoming = scheduleRepository.findByMovie_StatusAndEndDateAfter(true, threshold);

    return upcoming.stream()
        .filter(schedule -> schedule.getMovie() != null && schedule.getMovie().getId() != null)
        .filter(schedule -> movieIds.contains(schedule.getMovie().getId()))
        .collect(Collectors.groupingBy(schedule -> schedule.getMovie().getId()));
  }

  private Set<LocalDate> extractRequestedDates(String message, ZonedDateTime now) {
    if (!StringUtils.hasText(message)) {
      return Collections.emptySet();
    }
    Set<LocalDate> dates = new LinkedHashSet<>();
    String normalized = TextNormalizer.normalizeText(message);
    LocalDate today = now.toLocalDate();
    if (StringUtils.hasText(normalized)) {
      if (normalized.contains("hom nay") || normalized.contains("toi nay")) {
        dates.add(today);
      }
      if (normalized.contains("ngay mai") || normalized.contains("mai")) {
        dates.add(today.plusDays(1));
      }
      if (normalized.contains("cuoi tuan")) {
        dates.add(today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)));
        dates.add(today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
      }
    }

    Matcher isoMatcher = ISO_DATE_PATTERN.matcher(message);
    while (isoMatcher.find()) {
      int year = Integer.parseInt(isoMatcher.group(1));
      int month = Integer.parseInt(isoMatcher.group(2));
      int day = Integer.parseInt(isoMatcher.group(3));
      try {
        dates.add(LocalDate.of(year, month, day));
      } catch (DateTimeException ignored) {
        // ignore invalid date
      }
    }

    Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(message);
    while (shortMatcher.find()) {
      int first = Integer.parseInt(shortMatcher.group(1));
      int second = Integer.parseInt(shortMatcher.group(2));
      try {
        dates.add(LocalDate.of(now.getYear(), second, first));
      } catch (DateTimeException fallback) {
        try {
          dates.add(LocalDate.of(now.getYear(), first, second));
        } catch (DateTimeException ignored) {
          // ignore invalid date
        }
      }
    }
    return dates;
  }

  private Set<Integer> extractRequestedHours(String message) {
    if (!StringUtils.hasText(message)) {
      return Collections.emptySet();
    }
    Set<Integer> hours = new LinkedHashSet<>();

    Matcher hourMatcher = HOUR_PATTERN.matcher(message);
    while (hourMatcher.find()) {
      addHour(hours, hourMatcher.group(1));
    }

    Matcher colonMatcher = COLON_TIME_PATTERN.matcher(message);
    while (colonMatcher.find()) {
      addHour(hours, colonMatcher.group(1));
    }

    String normalized = TextNormalizer.normalizeText(message);
    if (StringUtils.hasText(normalized)) {
      if (normalized.contains("buoi toi") || normalized.contains("toi")) {
        hours.add(19);
      }
      if (normalized.contains("buoi chieu") || normalized.contains("chieu")) {
        hours.add(15);
      }
      if (normalized.contains("buoi sang") || normalized.contains("sang")) {
        hours.add(10);
      }
      if (normalized.contains("trua")) {
        hours.add(12);
      }
    }

    return hours;
  }

  private void addHour(Set<Integer> hours, String candidate) {
    int hour = Integer.parseInt(candidate);
    if (hour >= 0 && hour <= 23) {
      hours.add(hour);
    }
  }

  private String formatScore(double value) {
    return String.format(Locale.US, "%.2f", value);
  }

  public record RecommendationScoringInput(
      int groupMinimumAge,
      Set<String> preferredGenres,
      Set<String> keywordNamePatterns,
      String originalMessage,
      ZoneId zone,
      Set<LocalDate> requestedDates,
      Set<Integer> requestedHours) {}

  public record ScoredMovie(Movie movie, ScoreBreakdown breakdown) {}

  public record ScoreBreakdown(
      boolean ageAllowed,
      double ageScore,
      double genreScore,
      double nameScore,
      double ratingScore,
      double freshnessScore,
      double scheduleScore,
      double finalScore) {}

  public record QueryContext(Set<LocalDate> requestedDates, Set<Integer> requestedHours) {}
}
