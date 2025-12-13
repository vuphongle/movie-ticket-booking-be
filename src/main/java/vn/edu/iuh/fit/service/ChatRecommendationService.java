package vn.edu.iuh.fit.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import vn.edu.iuh.fit.entity.Auditorium;
import vn.edu.iuh.fit.entity.Cinema;
import vn.edu.iuh.fit.entity.Genre;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.entity.Showtime;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.model.enums.MovieAge;
import vn.edu.iuh.fit.model.request.ChatRecommendationRequest;
import vn.edu.iuh.fit.model.response.ChatRecommendationResponse;
import vn.edu.iuh.fit.model.response.RecommendedMovieResponse;
import vn.edu.iuh.fit.model.response.RecommendedShowtimeResponse;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.repository.ShowtimeRepository;
import vn.edu.iuh.fit.security.SecurityUtils;
import vn.edu.iuh.fit.service.chat.AgeRestrictionService;
import vn.edu.iuh.fit.service.chat.ChatCinemaLocator;
import vn.edu.iuh.fit.service.chat.ChatCinemaLocator.CinemaMatch;
import vn.edu.iuh.fit.service.chat.ChatMemoryService;
import vn.edu.iuh.fit.service.chat.ChatMemoryService.ChatMessage;
import vn.edu.iuh.fit.service.chat.ChatMemoryService.Role;
import vn.edu.iuh.fit.service.chat.KeywordAnalyzer;
import vn.edu.iuh.fit.service.chat.KeywordAnalyzer.KeywordContext;
import vn.edu.iuh.fit.service.chat.RecommendationScoringService;
import vn.edu.iuh.fit.service.chat.RecommendationScoringService.QueryContext;
import vn.edu.iuh.fit.service.chat.RecommendationScoringService.RecommendationScoringInput;
import vn.edu.iuh.fit.service.chat.RecommendationScoringService.ScoredMovie;
import vn.edu.iuh.fit.service.chat.TextNormalizer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRecommendationService {

  private static final int MAX_RECOMMENDATIONS = 5;
  private static final int MAX_SHOWTIMES = 3;
  private static final DateTimeFormatter SHOWTIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter FRIENDLY_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final String MISSING_AGE_MESSAGE =
      "Chúng tôi cần biết độ tuổi của bạn hoặc người đi cùng để gợi ý phim phù hợp. Vui lòng cập"
          + " nhật ngày sinh trong hồ sơ hoặc cung cấp trong tin nhắn.";

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final MovieRepository movieRepository;
  private final ShowtimeRepository showtimeRepository;
  private final KeywordAnalyzer keywordAnalyzer;
  private final RecommendationScoringService scoringService;
  private final AgeRestrictionService ageRestrictionService;
  private final ChatMemoryService chatMemoryService;
  private final ChatCinemaLocator cinemaLocator;
  private final Map<String, ChatMetadata> conversationMetadataCache = new ConcurrentHashMap<>();

  public ChatRecommendationResponse generateRecommendations(ChatRecommendationRequest request) {
    User currentUser = SecurityUtils.getCurrentUserLoginOptional().orElse(null);
    RecommendationContext context = prepareContext(request, currentUser);

    if (!context.hasAgeInfo()) {
      chatMemoryService.append(
          context.conversationId(),
          new ChatMessage(Role.USER, request.getMessage()),
          new ChatMessage(Role.ASSISTANT, MISSING_AGE_MESSAGE));
      return ChatRecommendationResponse.builder()
          .conversationId(context.conversationId())
          .answer(MISSING_AGE_MESSAGE)
          .recommendedMovies(Collections.emptyList())
          .build();
    }

    List<RecommendedMovieResponse> recommendationPayload = buildRecommendations(context);

    if (CollectionUtils.isEmpty(recommendationPayload)) {
      String fallback =
          context.enforceShowtimeFiltering()
              ? buildNoShowtimeFallback(context.requestedDates(), context.cinemaMatch())
              : "Hiện tại chúng tôi chưa tìm thấy phim phù hợp với độ tuổi và tiêu chí bạn yêu"
                  + " cầu. Bạn có thể thử lại với yêu cầu khác nhé.";
      chatMemoryService.append(
          context.conversationId(),
          new ChatMessage(Role.USER, request.getMessage()),
          new ChatMessage(Role.ASSISTANT, fallback));
      return ChatRecommendationResponse.builder()
          .conversationId(context.conversationId())
          .answer(fallback)
          .recommendedMovies(Collections.emptyList())
          .build();
    }

    String answer = callOpenAiAssistant(context, recommendationPayload);

    List<RecommendedMovieResponse> finalRecommendations =
        alignRecommendationsWithAnswer(answer, recommendationPayload);

    chatMemoryService.append(
        context.conversationId(),
        new ChatMessage(Role.USER, request.getMessage()),
        new ChatMessage(Role.ASSISTANT, answer));

    return ChatRecommendationResponse.builder()
        .conversationId(context.conversationId())
        .answer(answer)
        .recommendedMovies(finalRecommendations)
        .build();
  }

  private RecommendationContext prepareContext(
      ChatRecommendationRequest request, User currentUser) {
    ChatMetadata metadata = enrichMetadataWithUser(extractMetadata(request), currentUser);
    String conversationId = resolveConversationId(request, currentUser);
    String userName =
        currentUser != null && StringUtils.hasText(currentUser.getName())
            ? currentUser.getName()
            : null;
    metadata = mergeWithCachedMetadata(conversationId, metadata);
    List<ChatMessage> recentHistory = chatMemoryService.getRecentMessages(conversationId);
    String recentHistoryBlock = buildHistoryBlock(recentHistory);

    List<Integer> ageSamples = collectAllAges(metadata);
    List<Integer> immutableAges = List.copyOf(ageSamples);

    int groupMinAge =
        ageSamples.isEmpty() ? 0 : ageRestrictionService.determineMinimumAge(ageSamples);
    List<MovieAge> allowedRatings =
        ageSamples.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(ageRestrictionService.resolveAllowedRatings(groupMinAge));

    List<Movie> candidateMoviesRaw = movieRepository.findByStatusOrderByCreatedAtDesc(true);
    List<Movie> candidateMovies =
        candidateMoviesRaw == null ? Collections.emptyList() : List.copyOf(candidateMoviesRaw);

    KeywordContext keywordContext = keywordAnalyzer.analyze(request.getMessage());
    Set<String> preferredGenres =
        new LinkedHashSet<>(TextNormalizer.normalizePreferredGenres(metadata.preferredGenres()));
    preferredGenres.addAll(keywordContext.genreSlugs());

    Set<String> immutablePreferredGenres = toUnmodifiableLinkedSet(preferredGenres);
    Set<String> immutableKeywordPatterns = toUnmodifiableLinkedSet(keywordContext.namePatterns());

    ZonedDateTime referenceTime = ZonedDateTime.now(DEFAULT_ZONE);
    QueryContext queryContext =
        scoringService.extractQueryContext(request.getMessage(), referenceTime);

    Set<LocalDate> requestedDates =
        queryContext == null
            ? Collections.emptySet()
            : toUnmodifiableLinkedSet(queryContext.requestedDates());
    Set<Integer> requestedHours =
        queryContext == null
            ? Collections.emptySet()
            : toUnmodifiableLinkedSet(queryContext.requestedHours());

    CinemaMatch cinemaMatch = cinemaLocator.resolveCinema(request.getMessage()).orElse(null);
    Integer requestedCinemaId = cinemaMatch == null ? null : cinemaMatch.cinema().getId();
    boolean enforceShowtimeFiltering =
        !CollectionUtils.isEmpty(requestedDates) || cinemaMatch != null;

    RecommendationContext recommendationContext =
        new RecommendationContext(
            request,
            metadata,
            conversationId,
            userName,
            recentHistoryBlock,
            immutableAges,
            groupMinAge,
            allowedRatings,
            immutablePreferredGenres,
            immutableKeywordPatterns,
            referenceTime,
            requestedDates,
            requestedHours,
            cinemaMatch,
            requestedCinemaId,
            enforceShowtimeFiltering,
            candidateMovies);
    cacheMetadata(conversationId, metadata);
    return recommendationContext;
  }

  private List<RecommendedMovieResponse> buildRecommendations(RecommendationContext context) {
    RecommendationScoringInput scoringInput =
        new RecommendationScoringInput(
            context.groupMinimumAge(),
            context.preferredGenres(),
            context.keywordPatterns(),
            context.request().getMessage(),
            DEFAULT_ZONE,
            context.requestedDates(),
            context.requestedHours());

    List<ScoredMovie> scoredMovies =
        scoringService.scoreMovies(context.candidateMovies(), scoringInput);
    scoringService.logTopCandidates(scoredMovies);

    List<ScoredMovie> orderedCandidates =
        scoredMovies.stream()
            .filter(candidate -> candidate.breakdown().ageAllowed())
            .sorted(
                Comparator.comparingDouble(
                        (ScoredMovie candidate) -> candidate.breakdown().finalScore())
                    .reversed()
                    .thenComparing(
                        candidate -> candidate.movie().getRating(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        candidate -> candidate.movie().getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    List<RecommendedMovieResponse> recommendationPayload = new ArrayList<>();
    for (ScoredMovie scored : orderedCandidates) {
      Movie movie = scored.movie();
      List<Showtime> matchingShowtimes =
          findRelevantShowtimes(
              movie,
              context.requestedDates(),
              context.requestedHours(),
              context.requestedCinemaId(),
              context.referenceTime());
      if (context.enforceShowtimeFiltering() && CollectionUtils.isEmpty(matchingShowtimes)) {
        continue;
      }
      recommendationPayload.add(buildRecommendedMovie(movie, matchingShowtimes));
      if (recommendationPayload.size() >= MAX_RECOMMENDATIONS) {
        break;
      }
    }
    return recommendationPayload;
  }

  private ChatMetadata enrichMetadataWithUser(ChatMetadata metadata, User currentUser) {
    if (metadata == null) {
      metadata = ChatMetadata.empty();
    }

    Integer effectiveUserAge = metadata.userAge();
    if (effectiveUserAge == null) {
      effectiveUserAge = calculateAge(currentUser == null ? null : currentUser.getDob());
    }

    List<Integer> companionAges =
        metadata.companionAges() == null ? Collections.emptyList() : metadata.companionAges();

    List<String> preferredGenres =
        metadata.preferredGenres() == null ? Collections.emptyList() : metadata.preferredGenres();

    return new ChatMetadata(effectiveUserAge, companionAges, preferredGenres);
  }

  private ChatMetadata mergeWithCachedMetadata(String conversationId, ChatMetadata metadata) {
    ChatMetadata sanitized = sanitizeMetadata(metadata);
    if (!StringUtils.hasText(conversationId)) {
      return sanitized;
    }
    ChatMetadata cached = conversationMetadataCache.get(conversationId);
    if (cached == null) {
      return sanitized;
    }
    Integer userAge = sanitized.userAge() != null ? sanitized.userAge() : cached.userAge();
    List<Integer> companionAges = mergeAges(cached.companionAges(), sanitized.companionAges());
    List<String> preferredGenres =
        mergeGenres(cached.preferredGenres(), sanitized.preferredGenres());
    return new ChatMetadata(userAge, companionAges, preferredGenres);
  }

  private void cacheMetadata(String conversationId, ChatMetadata metadata) {
    if (!StringUtils.hasText(conversationId) || metadata == null) {
      return;
    }
    ChatMetadata sanitized = sanitizeMetadata(metadata);
    conversationMetadataCache.merge(
        conversationId,
        sanitized,
        (existing, incoming) -> {
          Integer userAge = incoming.userAge() != null ? incoming.userAge() : existing.userAge();
          List<Integer> companionAges =
              mergeAges(existing.companionAges(), incoming.companionAges());
          List<String> preferredGenres =
              mergeGenres(existing.preferredGenres(), incoming.preferredGenres());
          return new ChatMetadata(userAge, companionAges, preferredGenres);
        });
  }

  private ChatMetadata sanitizeMetadata(ChatMetadata metadata) {
    if (metadata == null) {
      return ChatMetadata.empty();
    }
    Integer userAge = metadata.userAge();
    List<Integer> companionAges = mergeAges(metadata.companionAges());
    List<String> preferredGenres = mergeGenres(metadata.preferredGenres());
    return new ChatMetadata(userAge, companionAges, preferredGenres);
  }

  @SafeVarargs
  private final List<Integer> mergeAges(List<Integer>... sources) {
    LinkedHashSet<Integer> merged = new LinkedHashSet<>();
    if (sources != null) {
      for (List<Integer> source : sources) {
        if (CollectionUtils.isEmpty(source)) {
          continue;
        }
        source.stream().filter(Objects::nonNull).filter(age -> age > 0).forEach(merged::add);
      }
    }
    return merged.isEmpty() ? Collections.emptyList() : List.copyOf(merged);
  }

  @SafeVarargs
  private final List<String> mergeGenres(List<String>... sources) {
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    if (sources != null) {
      for (List<String> source : sources) {
        if (CollectionUtils.isEmpty(source)) {
          continue;
        }
        source.stream().filter(StringUtils::hasText).map(String::trim).forEach(merged::add);
      }
    }
    return merged.isEmpty() ? Collections.emptyList() : List.copyOf(merged);
  }

  private Integer calculateAge(Date dob) {
    if (dob == null) {
      return null;
    }
    LocalDate birthDate;
    if (dob instanceof java.sql.Date sqlDate) {
      birthDate = sqlDate.toLocalDate();
    } else {
      birthDate = dob.toInstant().atZone(DEFAULT_ZONE).toLocalDate();
    }
    LocalDate today = LocalDate.now(DEFAULT_ZONE);
    if (birthDate.isAfter(today)) {
      return null;
    }
    return Period.between(birthDate, today).getYears();
  }

  private String callOpenAiAssistant(
      RecommendationContext context, List<RecommendedMovieResponse> recommendations) {
    try {
      String language = determineLanguage(context.request());
      String userName = context.userName();

      String greetingInstruction = "";
      if (StringUtils.hasText(userName)) {
        greetingInstruction =
            " Nếu đây là tin nhắn đầu tiên hoặc câu chào, hãy chào người dùng bằng tên '"
                + userName
                + "' một cách thân thiện (ví dụ: 'Chào "
                + userName
                + "!')."
                + " Nếu đang trong cuộc hội thoại, không cần chào lại.";
      }

      String systemPrompt =
          "Bạn là trợ lý tư vấn phim cho rạp chiếu phim Việt Nam. Chỉ sử dụng danh sách phim do hệ"
              + " thống cung cấp trong phiên này, tuyệt đối không bịa thêm phim hay thông tin mới."
              + " Nếu không có phim phù hợp, hãy nói rõ 'chưa tìm thấy phim phù hợp' và gợi ý người"
              + " dùng thay đổi tiêu chí (ví dụ: thể loại, thời gian chiếu, độ tuổi). Giữ giọng"
              + " điệu thân thiện, súc tích và trả lời bằng ngôn ngữ người dùng yêu cầu. Luôn tôn"
              + " trọng hệ thống phân loại độ tuổi Việt Nam (P, K, T13, T16, T18) và không gợi ý"
              + " phim vượt giới hạn. Nếu câu hỏi lệch khỏi chủ đề phim, hãy khéo léo đưa người"
              + " dùng trở lại với những gợi ý phim."
              + greetingInstruction;

      String userPrompt = buildUserPrompt(context, recommendations, language);

      String content =
          chatClient
              .prompt()
              .options(OpenAiChatOptions.builder().temperature(0.2).build())
              .system(systemPrompt)
              .user(userPrompt)
              .call()
              .content();

      if (!StringUtils.hasText(content) || isLowQualityAnswer(content, recommendations)) {
        log.warn("AI trả lời chưa đạt yêu cầu, sử dụng fallback được tổng hợp nội bộ");
        return generateFallbackAnswer(recommendations, language);
      }
      return content;
    } catch (Exception ex) {
      log.error("AI assistant failed to answer", ex);
      return generateFallbackAnswer(recommendations, determineLanguage(context.request()));
    }
  }

  private String generateFallbackAnswer(
      List<RecommendedMovieResponse> recommendations, String language) {
    if (CollectionUtils.isEmpty(recommendations)) {
      return "Hiện tại chúng tôi chưa tìm thấy phim phù hợp với độ tuổi và tiêu chí bạn yêu cầu."
          + " Bạn có thể thử lại với yêu cầu khác nhé.";
    }
    boolean isEnglish =
        StringUtils.hasText(language) && language.toLowerCase(Locale.ROOT).startsWith("en");
    String intro =
        isEnglish
            ? "Here are some movies that fit your preferences:"
            : "Dưới đây là những bộ phim mà chúng tôi thấy phù hợp với bạn:";
    String body =
        recommendations.stream()
            .limit(3)
            .map(movie -> formatFallbackLine(movie, isEnglish))
            .collect(Collectors.joining("\n"));
    String outro =
        isEnglish
            ? "Let me know if you want to book tickets or refine the search!"
            : "Nếu bạn muốn đặt vé hoặc tìm thêm phim khác, hãy cho tôi biết nhé!";
    return intro + "\n" + body + "\n" + outro;
  }

  private String determineLanguage(ChatRecommendationRequest request) {
    if (request == null || !StringUtils.hasText(request.getLanguage())) {
      return "vi";
    }
    return request.getLanguage();
  }

  private String formatFallbackLine(RecommendedMovieResponse movie, boolean isEnglish) {
    StringBuilder builder = new StringBuilder();
    builder.append(isEnglish ? "- " : "- ");
    builder.append(movie.getName());
    if (movie.getAgeRating() != null) {
      builder.append(" (").append(movie.getAgeRating()).append(")");
    }
    if (movie.getRating() != null) {
      builder
          .append(isEnglish ? ", rating " : ", đánh giá ")
          .append(String.format(Locale.US, "%.1f", movie.getRating()));
    }
    if (!CollectionUtils.isEmpty(movie.getGenreDisplayNames()) && !isEnglish) {
      builder.append(", thể loại: ").append(String.join(", ", movie.getGenreDisplayNames()));
    } else if (!CollectionUtils.isEmpty(movie.getGenreDisplayNames()) && isEnglish) {
      builder.append(", genres: ").append(String.join(", ", movie.getGenreDisplayNames()));
    }
    if (!CollectionUtils.isEmpty(movie.getShowtimes())) {
      String showtimeText =
          movie.getShowtimes().stream()
              .map(this::formatShowtimeDisplay)
              .filter(StringUtils::hasText)
              .limit(MAX_SHOWTIMES)
              .collect(Collectors.joining("; "));
      if (StringUtils.hasText(showtimeText)) {
        builder.append(isEnglish ? ", showtimes: " : ", suất chiếu: ").append(showtimeText);
      }
    }
    return builder.toString();
  }

  private boolean isLowQualityAnswer(
      String answer, List<RecommendedMovieResponse> recommendations) {
    if (!StringUtils.hasText(answer)) {
      return true;
    }
    String normalizedAnswer = normalizeForComparison(answer);
    List<String> negativeSignals =
        List.of("xin-loi", "khong-the-giup", "ngoai-pham-vi", "cannot-help", "sorry");
    if (negativeSignals.stream().anyMatch(normalizedAnswer::contains)) {
      return true;
    }
    if (CollectionUtils.isEmpty(recommendations)) {
      return false;
    }
    return recommendations.stream()
        .map(RecommendedMovieResponse::getName)
        .filter(StringUtils::hasText)
        .map(this::normalizeForComparison)
        .noneMatch(normalizedAnswer::contains);
  }

  private String normalizeForComparison(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String slug = TextNormalizer.toSlug(value);
    return slug == null ? "" : slug;
  }

  private ChatMetadata extractMetadata(ChatRecommendationRequest request) {
    String systemPrompt =
        "Bạn là chuyên gia trích xuất thông tin từ yêu cầu tư vấn phim. Hãy trả về JSON hợp lệ với"
            + " khóa: userAge (number hoặc null), companionAges (array số), preferredGenres (array"
            + " chuỗi viết hoa không dấu).";

    String userPrompt = "Tin nhắn người dùng: " + request.getMessage();

    try {
      var response = chatClient.prompt().system(systemPrompt).user(userPrompt).call();

      try {
        ChatMetadata structured = response.entity(ChatMetadata.class);
        if (structured != null) {
          return structured;
        }
      } catch (Exception entityEx) {
        log.debug(
            "Structured metadata parsing thất bại, fallback sang phân tích thủ công", entityEx);
      }

      String raw = response.content();
      String json = extractJsonBlock(raw);
      ChatMetadata metadata = objectMapper.readValue(json, ChatMetadata.class);
      return metadata == null ? ChatMetadata.empty() : metadata;
    } catch (Exception ex) {
      log.warn("Không thể phân tích metadata từ AI, sử dụng giá trị mặc định", ex);
      return ChatMetadata.empty();
    }
  }

  private String extractJsonBlock(String content) {
    if (!StringUtils.hasText(content)) {
      return "{}";
    }
    String trimmed = content.trim();
    int start = -1;
    int depth = 0;
    boolean inString = false;
    char prev = 0;
    for (int i = 0; i < trimmed.length(); i++) {
      char current = trimmed.charAt(i);
      if (current == '"' && prev != '\\') {
        inString = !inString;
      }
      if (!inString) {
        if (current == '{') {
          if (depth == 0) {
            start = i;
          }
          depth++;
        } else if (current == '}') {
          if (depth > 0) {
            depth--;
            if (depth == 0 && start != -1) {
              return trimmed.substring(start, i + 1);
            }
          }
        }
      }
      prev = current;
    }
    return "{}";
  }

  private RecommendedMovieResponse buildRecommendedMovie(Movie movie, List<Showtime> showtimes) {
    List<String> reasons = new ArrayList<>();
    if (movie.getAge() != null) {
      reasons.add("Phân loại độ tuổi: " + movie.getAge().name());
    }
    if (movie.getRating() != null) {
      reasons.add(String.format(Locale.US, "Đánh giá trung bình: %.1f", movie.getRating()));
    }
    if (movie.getDuration() != null) {
      reasons.add("Thời lượng: " + movie.getDuration() + " phút");
    }

    List<String> showtimeSummaries =
        showtimes == null
            ? Collections.emptyList()
            : showtimes.stream()
                .map(this::formatShowtimeDisplay)
                .filter(StringUtils::hasText)
                .limit(MAX_SHOWTIMES)
                .toList();

    List<String> genreSlugs =
        CollectionUtils.isEmpty(movie.getGenres())
            ? Collections.emptyList()
            : movie.getGenres().stream()
                .map(
                    genre ->
                        StringUtils.hasText(genre.getSlug()) ? genre.getSlug() : genre.getName())
                .filter(StringUtils::hasText)
                .map(TextNormalizer::toSlug)
                .filter(StringUtils::hasText)
                .toList();

    List<String> genreDisplayNames =
        CollectionUtils.isEmpty(movie.getGenres())
            ? Collections.emptyList()
            : movie.getGenres().stream().map(Genre::getName).filter(StringUtils::hasText).toList();

    List<RecommendedShowtimeResponse> showtimeDetails =
        showtimes == null
            ? Collections.emptyList()
            : showtimes.stream()
                .map(this::mapShowtimeToRecommendation)
                .filter(Objects::nonNull)
                .limit(MAX_SHOWTIMES)
                .toList();

    return RecommendedMovieResponse.builder()
        .movieId(movie.getId())
        .name(movie.getName())
        .slug(
            StringUtils.hasText(movie.getSlug())
                ? movie.getSlug()
                : TextNormalizer.toSlug(movie.getName()))
        .poster(movie.getPoster())
        .ageRating(movie.getAge())
        .rating(movie.getRating())
        .genres(genreSlugs)
        .genreDisplayNames(genreDisplayNames)
        .reasons(reasons)
        .showtimes(showtimeDetails)
        .build();
  }

  private RecommendedShowtimeResponse mapShowtimeToRecommendation(Showtime showtime) {
    if (showtime == null || showtime.getId() == null) {
      return null;
    }
    Auditorium auditorium = showtime.getAuditorium();
    Cinema cinema = auditorium == null ? null : auditorium.getCinema();

    return RecommendedShowtimeResponse.builder()
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
        .auditoriumType(
            auditorium != null && auditorium.getType() != null ? auditorium.getType().name() : null)
        .build();
  }

  private List<Showtime> findRelevantShowtimes(
      Movie movie,
      Set<LocalDate> requestedDates,
      Set<Integer> requestedHours,
      Integer requestedCinemaId,
      ZonedDateTime referenceTime) {
    if (movie == null || movie.getId() == null) {
      return Collections.emptyList();
    }

    LocalDate today = referenceTime.toLocalDate();
    List<Showtime> upcomingShowtimes =
        showtimeRepository.findByMovie_IdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            movie.getId(), today);

    if (CollectionUtils.isEmpty(upcomingShowtimes)) {
      upcomingShowtimes = showtimeRepository.findByMovie_Id(movie.getId());
    }

    if (CollectionUtils.isEmpty(upcomingShowtimes)) {
      return Collections.emptyList();
    }

    LocalTime currentTime = referenceTime.toLocalTime();

    List<Showtime> filtered =
        upcomingShowtimes.stream()
            .filter(Objects::nonNull)
            .filter(showtime -> showtime.getDate() != null)
            .filter(showtime -> matchesCinema(showtime, requestedCinemaId))
            .filter(showtime -> isUpcoming(showtime, today, currentTime))
            .collect(Collectors.toCollection(ArrayList::new));

    if (!CollectionUtils.isEmpty(requestedDates)) {
      filtered =
          filtered.stream()
              .filter(showtime -> requestedDates.contains(showtime.getDate()))
              .collect(Collectors.toCollection(ArrayList::new));
    }

    if (CollectionUtils.isEmpty(filtered)) {
      return Collections.emptyList();
    }

    List<Showtime> hourMatched = filterByRequestedHours(filtered, requestedHours);
    List<Showtime> prioritized =
        !CollectionUtils.isEmpty(requestedHours) && !CollectionUtils.isEmpty(hourMatched)
            ? hourMatched
            : filtered;

    LinkedHashMap<ZonedDateTime, Showtime> unique = new LinkedHashMap<>();
    for (Showtime showtime :
        prioritized.stream()
            .sorted(
                Comparator.comparing(
                    this::resolveShowtimeStart, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList()) {
      ZonedDateTime start = resolveShowtimeStart(showtime);
      if (start == null) {
        continue;
      }
      unique.putIfAbsent(start, showtime);
      if (unique.size() >= MAX_SHOWTIMES * 3) {
        break;
      }
    }

    return new ArrayList<>(unique.values());
  }

  private boolean matchesCinema(Showtime showtime, Integer cinemaId) {
    if (cinemaId == null) {
      return true;
    }
    if (showtime == null || showtime.getAuditorium() == null) {
      return false;
    }
    var cinema = showtime.getAuditorium().getCinema();
    return cinema != null && cinemaId.equals(cinema.getId());
  }

  private boolean isUpcoming(Showtime showtime, LocalDate today, LocalTime currentTime) {
    LocalDate date = showtime.getDate();
    if (date == null) {
      return false;
    }
    if (date.isBefore(today)) {
      return false;
    }
    if (date.isAfter(today)) {
      return true;
    }
    LocalTime startTime = parseStartTime(showtime.getStartTime());
    if (startTime == null) {
      return true;
    }
    return !startTime.isBefore(currentTime);
  }

  private List<Showtime> filterByRequestedHours(
      List<Showtime> showtimes, Set<Integer> requestedHours) {
    if (CollectionUtils.isEmpty(requestedHours) || CollectionUtils.isEmpty(showtimes)) {
      return Collections.emptyList();
    }
    return showtimes.stream()
        .filter(Objects::nonNull)
        .filter(
            showtime -> {
              LocalTime time = parseStartTime(showtime.getStartTime());
              if (time == null) {
                return false;
              }
              int hour = time.getHour();
              return requestedHours.stream().anyMatch(requested -> Math.abs(requested - hour) <= 1);
            })
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private ZonedDateTime resolveShowtimeStart(Showtime showtime) {
    if (showtime == null || showtime.getDate() == null) {
      return null;
    }
    LocalTime time = parseStartTime(showtime.getStartTime());
    if (time == null) {
      time = LocalTime.of(0, 0);
    }
    return ZonedDateTime.of(showtime.getDate(), time, DEFAULT_ZONE);
  }

  private LocalTime parseStartTime(String startTime) {
    if (!StringUtils.hasText(startTime)) {
      return null;
    }
    try {
      return LocalTime.parse(startTime);
    } catch (DateTimeParseException ex) {
      String normalized = startTime.replace("h", ":");
      try {
        return LocalTime.parse(normalized);
      } catch (DateTimeParseException ignored) {
        return null;
      }
    }
  }

  private String formatShowtimeDisplay(Showtime showtime) {
    ZonedDateTime start = resolveShowtimeStart(showtime);
    if (start == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder(SHOWTIME_FORMATTER.format(start));
    if (showtime.getAuditorium() != null && showtime.getAuditorium().getCinema() != null) {
      builder.append(" • ").append(showtime.getAuditorium().getCinema().getName());
    }
    if (showtime.getAuditorium() != null
        && StringUtils.hasText(showtime.getAuditorium().getName())) {
      builder.append(" • ").append(showtime.getAuditorium().getName());
    }
    if (showtime.getGraphicsType() != null) {
      builder.append(" • ").append(showtime.getGraphicsType());
    }
    if (showtime.getTranslationType() != null) {
      builder.append(" • ").append(showtime.getTranslationType());
    }
    return builder.toString();
  }

  private String formatShowtimeDisplay(RecommendedShowtimeResponse showtime) {
    if (showtime == null || showtime.getDate() == null) {
      return null;
    }
    LocalTime time = parseStartTime(showtime.getStartTime());
    ZonedDateTime start =
        ZonedDateTime.of(
            showtime.getDate(), time == null ? LocalTime.MIDNIGHT : time, DEFAULT_ZONE);
    StringBuilder builder = new StringBuilder(SHOWTIME_FORMATTER.format(start));
    if (StringUtils.hasText(showtime.getCinemaName())) {
      builder.append(" • ").append(showtime.getCinemaName());
    }
    if (StringUtils.hasText(showtime.getAuditoriumName())) {
      builder.append(" • ").append(showtime.getAuditoriumName());
    }
    if (StringUtils.hasText(showtime.getGraphicsType())) {
      builder.append(" • ").append(showtime.getGraphicsType());
    }
    if (StringUtils.hasText(showtime.getTranslationType())) {
      builder.append(" • ").append(showtime.getTranslationType());
    }
    return builder.toString();
  }

  private String buildNoShowtimeFallback(Set<LocalDate> requestedDates, CinemaMatch cinemaMatch) {
    String cinemaText =
        cinemaMatch == null || cinemaMatch.cinema() == null ? null : cinemaMatch.cinema().getName();
    if (!CollectionUtils.isEmpty(requestedDates) && StringUtils.hasText(cinemaText)) {
      return "Chúng tôi chưa tìm thấy suất chiếu cho "
          + cinemaText
          + " vào các ngày "
          + requestedDates.stream()
              .sorted()
              .map(date -> date.format(FRIENDLY_DATE_FORMAT))
              .collect(Collectors.joining(", "))
          + ". Bạn có thể thử ngày hoặc rạp khác nhé.";
    }
    if (!CollectionUtils.isEmpty(requestedDates)) {
      return "Hiện chưa có suất chiếu vào các ngày "
          + requestedDates.stream()
              .sorted()
              .map(date -> date.format(FRIENDLY_DATE_FORMAT))
              .collect(Collectors.joining(", "))
          + ". Bạn có thể chọn ngày khác hoặc mở rộng tìm kiếm.";
    }
    if (StringUtils.hasText(cinemaText)) {
      return "Chúng tôi chưa thấy suất chiếu nào tại "
          + cinemaText
          + " vào thời gian bạn yêu cầu. Bạn có thể thử rạp khác hoặc đặt câu hỏi khác nhé.";
    }
    return "Hiện tại chúng tôi chưa tìm thấy suất chiếu phù hợp. Bạn có thể thử lại với tiêu chí khác.";
  }

  private String buildUserPrompt(
      RecommendationContext context,
      List<RecommendedMovieResponse> recommendations,
      String language) {
    ChatMetadata metadata = context.metadata();
    String recentHistory = context.recentHistoryBlock();
    String historySection =
        StringUtils.hasText(recentHistory)
            ? recentHistory
            : "Không có hội thoại gần đây hoặc chưa lưu được.";

    String userNameInfo =
        StringUtils.hasText(context.userName())
            ? "Tên người dùng: " + context.userName() + "\n"
            : "";

    String moviesContext =
        recommendations.stream()
            .map(
                movie -> {
                  String genreSlugText =
                      CollectionUtils.isEmpty(movie.getGenres())
                          ? "không xác định"
                          : String.join(", ", movie.getGenres());
                  String genreNameText =
                      CollectionUtils.isEmpty(movie.getGenreDisplayNames())
                          ? ""
                          : " | tên: " + String.join(", ", movie.getGenreDisplayNames());
                  String ratingText =
                      movie.getRating() != null
                          ? String.format(Locale.US, ", rating %.1f", movie.getRating())
                          : "";
                  String showtimeText;
                  if (CollectionUtils.isEmpty(movie.getShowtimes())) {
                    showtimeText = "suất chiếu: chưa xác định";
                  } else {
                    String formattedShowtimes =
                        movie.getShowtimes().stream()
                            .map(this::formatShowtimeDisplay)
                            .filter(StringUtils::hasText)
                            .limit(MAX_SHOWTIMES)
                            .collect(Collectors.joining("; "));
                    showtimeText =
                        StringUtils.hasText(formattedShowtimes)
                            ? "suất chiếu: " + formattedShowtimes
                            : "suất chiếu: chưa xác định";
                  }
                  return "- "
                      + movie.getName()
                      + " ("
                      + movie.getAgeRating()
                      + ratingText
                      + ", slug thể loại: "
                      + genreSlugText
                      + genreNameText
                      + ") "
                      + showtimeText;
                })
            .collect(Collectors.joining("\n"));

    List<Integer> companionAgeList =
        metadata.companionAges() == null ? Collections.emptyList() : metadata.companionAges();

    String companionAges =
        CollectionUtils.isEmpty(companionAgeList)
            ? "Không cung cấp"
            : companionAgeList.stream().map(String::valueOf).collect(Collectors.joining(", "));

    String userAge =
        metadata.userAge() == null ? "Không cung cấp" : String.valueOf(metadata.userAge());

    String allowedText =
        context.allowedRatings().stream().map(Enum::name).collect(Collectors.joining(", "));

    String genreHintText =
        CollectionUtils.isEmpty(context.preferredGenres())
            ? "Không cung cấp"
            : String.join(", ", context.preferredGenres());

    String keywordHintText =
        CollectionUtils.isEmpty(context.keywordPatterns())
            ? "Không xác định"
            : context.keywordPatterns().stream()
                .map(pattern -> pattern.replace('-', ' '))
                .collect(Collectors.joining(", "));

    String requestedDateText =
        CollectionUtils.isEmpty(context.requestedDates())
            ? "Không cung cấp"
            : context.requestedDates().stream()
                .sorted()
                .map(date -> date.format(FRIENDLY_DATE_FORMAT))
                .collect(Collectors.joining(", "));

    String cinemaText =
        context.cinemaMatch() == null || context.cinemaMatch().cinema() == null
            ? "Không cung cấp"
            : context.cinemaMatch().cinema().getName();

    return "Lịch sử hội thoại gần đây (tối đa 5 lượt):\n"
        + historySection
        + "\n\n"
        + userNameInfo
        + "Người dùng hỏi bằng ngôn ngữ: "
        + language
        + "\n"
        + "Tin nhắn của người dùng: "
        + context.request().getMessage()
        + "\n"
        + "Độ tuổi của người hỏi: "
        + userAge
        + "\n"
        + "Độ tuổi người đi cùng: "
        + companionAges
        + "\n"
        + "Các phân loại độ tuổi được phép: "
        + allowedText
        + "\n"
        + "Các thể loại ưu tiên (bao gồm suy luận từ từ khóa): "
        + genreHintText
        + "\n"
        + "Từ khóa nổi bật nhận được: "
        + keywordHintText
        + "\n"
        + "Ngày được yêu cầu: "
        + requestedDateText
        + "\n"
        + "Rạp được yêu cầu: "
        + cinemaText
        + "\n"
        + "Danh sách phim có thể gợi ý (tối đa "
        + MAX_RECOMMENDATIONS
        + "):\n"
        + moviesContext
        + "\n"
        + "Hãy phản hồi tối đa 2 đoạn ngắn, giữ thân thiện, nhắc đến 2-3 phim tiêu biểu và khuyến khích người dùng đặt vé.\n"
        + "Nếu có thông tin suất chiếu, hãy nêu rõ thời gian và rạp tương ứng.\n"
        + "Chỉ đề cập tới các phim trong danh sách, không bịa thêm nội dung. Nếu thông tin chưa đủ, hãy gợi ý người dùng cung cấp thêm tiêu chí.";
  }

  private static <T> Set<T> toUnmodifiableLinkedSet(Collection<T> source) {
    if (source == null || source.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(new LinkedHashSet<>(source));
  }

  private List<RecommendedMovieResponse> alignRecommendationsWithAnswer(
      String answer, List<RecommendedMovieResponse> recommendations) {
    if (!StringUtils.hasText(answer) || CollectionUtils.isEmpty(recommendations)) {
      return Collections.emptyList();
    }
    String normalizedAnswer = normalizeForComparison(answer);
    if (!StringUtils.hasText(normalizedAnswer)) {
      return Collections.emptyList();
    }

    List<RecommendationMatch> matches = new ArrayList<>();
    for (int i = 0; i < recommendations.size(); i++) {
      RecommendedMovieResponse recommendation = recommendations.get(i);
      String movieName = recommendation.getName();
      if (!StringUtils.hasText(movieName)) {
        continue;
      }
      String normalizedName = normalizeForComparison(movieName);
      if (!StringUtils.hasText(normalizedName)) {
        continue;
      }
      int index = normalizedAnswer.indexOf(normalizedName);
      if (index >= 0) {
        matches.add(new RecommendationMatch(recommendation, index, i));
      }
    }

    if (matches.isEmpty()) {
      return Collections.emptyList();
    }

    matches.sort(
        Comparator.comparingInt(RecommendationMatch::position)
            .thenComparingInt(RecommendationMatch::originalIndex));
    return matches.stream().map(RecommendationMatch::recommendation).toList();
  }

  private record RecommendationMatch(
      RecommendedMovieResponse recommendation, int position, int originalIndex) {}

  private record RecommendationContext(
      ChatRecommendationRequest request,
      ChatMetadata metadata,
      String conversationId,
      String userName,
      String recentHistoryBlock,
      List<Integer> ageSamples,
      int groupMinimumAge,
      List<MovieAge> allowedRatings,
      Set<String> preferredGenres,
      Set<String> keywordPatterns,
      ZonedDateTime referenceTime,
      Set<LocalDate> requestedDates,
      Set<Integer> requestedHours,
      CinemaMatch cinemaMatch,
      Integer requestedCinemaId,
      boolean enforceShowtimeFiltering,
      List<Movie> candidateMovies) {

    boolean hasAgeInfo() {
      return ageSamples != null && !ageSamples.isEmpty();
    }
  }

  private List<Integer> collectAllAges(ChatMetadata metadata) {
    List<Integer> ages = new ArrayList<>();
    if (metadata.userAge() != null && metadata.userAge() > 0) {
      ages.add(metadata.userAge());
    }
    List<Integer> companionAges =
        metadata.companionAges() == null ? Collections.emptyList() : metadata.companionAges();
    if (!CollectionUtils.isEmpty(companionAges)) {
      ages.addAll(companionAges.stream().filter(Objects::nonNull).filter(age -> age > 0).toList());
    }
    return ages;
  }

  private String resolveConversationId(ChatRecommendationRequest request, User currentUser) {
    if (request != null && StringUtils.hasText(request.getConversationId())) {
      return request.getConversationId().trim();
    }
    if (currentUser != null && currentUser.getId() != null) {
      return "user-" + currentUser.getId();
    }
    return "guest-" + UUID.randomUUID();
  }

  private String buildHistoryBlock(List<ChatMessage> history) {
    if (CollectionUtils.isEmpty(history)) {
      return null;
    }
    return history.stream()
        .map(
            entry -> (entry.role() == Role.USER ? "Người dùng" : "Trợ lý") + ": " + entry.content())
        .collect(Collectors.joining("\n"));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatMetadata(Integer userAge, List<Integer> companionAges, List<String> preferredGenres) {
    static ChatMetadata empty() {
      return new ChatMetadata(null, Collections.emptyList(), Collections.emptyList());
    }
  }
}
