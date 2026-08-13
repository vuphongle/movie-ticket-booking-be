package vn.edu.iuh.fit.service.chat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import vn.edu.iuh.fit.entity.Cinema;
import vn.edu.iuh.fit.repository.CinemaRepository;

@Component
@RequiredArgsConstructor
public class ChatCinemaLocator {

  private static final double DIRECT_MATCH_SCORE = 1.0;
  private static final double NAME_CONTAINS_SCORE = 0.85;
  private static final double MIN_CONFIDENCE = 0.4;

  private final CinemaRepository cinemaRepository;

  public Optional<CinemaMatch> resolveCinema(String message) {
    if (!StringUtils.hasText(message)) {
      return Optional.empty();
    }
    String normalizedMessage = TextNormalizer.normalizeText(message);
    String slugMessage = TextNormalizer.toSlug(message);
    Set<String> messageTokens = tokenize(normalizedMessage);

    List<Cinema> cinemas = cinemaRepository.findAll();
    if (CollectionUtils.isEmpty(cinemas)) {
      return Optional.empty();
    }

    return cinemas.stream()
        .map(
            cinema ->
                new CinemaMatch(
                    cinema,
                    calculateConfidence(cinema, normalizedMessage, slugMessage, messageTokens)))
        .filter(match -> match.confidence() >= MIN_CONFIDENCE)
        .max(Comparator.comparingDouble(CinemaMatch::confidence));
  }

  private double calculateConfidence(
      Cinema cinema, String normalizedMessage, String slugMessage, Set<String> messageTokens) {
    if (cinema == null) {
      return 0.0;
    }

    double score = 0.0;

    String nameSlug = TextNormalizer.toSlug(cinema.getName());
    if (StringUtils.hasText(nameSlug) && slugMessage != null && slugMessage.contains(nameSlug)) {
      return DIRECT_MATCH_SCORE;
    }

    String normalizedName = TextNormalizer.normalizeText(cinema.getName());
    score = Math.max(score, overlapScore(normalizedMessage, messageTokens, normalizedName));

    String normalizedAddress = TextNormalizer.normalizeText(cinema.getAddress());
    score =
        Math.max(score, 0.8 * overlapScore(normalizedMessage, messageTokens, normalizedAddress));

    return score;
  }

  private double overlapScore(
      String normalizedMessage, Set<String> messageTokens, String candidatePhrase) {
    if (!StringUtils.hasText(normalizedMessage) || !StringUtils.hasText(candidatePhrase)) {
      return 0.0;
    }

    if (normalizedMessage.contains(candidatePhrase)) {
      return NAME_CONTAINS_SCORE;
    }

    Set<String> candidateTokens = tokenize(candidatePhrase);
    if (CollectionUtils.isEmpty(candidateTokens) || CollectionUtils.isEmpty(messageTokens)) {
      return 0.0;
    }

    long matches = candidateTokens.stream().filter(messageTokens::contains).count();
    if (matches == 0) {
      return 0.0;
    }
    double ratio = (double) matches / candidateTokens.size();
    return Math.min(0.9, ratio);
  }

  private Set<String> tokenize(String input) {
    if (!StringUtils.hasText(input)) {
      return Collections.emptySet();
    }
    return Arrays.stream(input.split(" "))
        .map(String::trim)
        .filter(token -> token.length() >= 2)
        .map(TextNormalizer::normalizeText)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  public record CinemaMatch(Cinema cinema, double confidence) {}
}
