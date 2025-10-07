package vn.edu.iuh.fit.service.chat;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Utility helpers for normalising user-provided text into stable forms. */
public final class TextNormalizer {

  private TextNormalizer() {
    // utility
  }

  public static String normalizeText(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String normalized =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace('Đ', 'D')
            .replace('đ', 'd')
            .replaceAll("\\p{M}+", "");
    String ascii = normalized.replaceAll("[^a-zA-Z0-9\\s]", " ");
    String collapsed = ascii.trim().replaceAll("[\\s_]+", " ");
    if (!StringUtils.hasText(collapsed)) {
      return null;
    }
    return collapsed.toLowerCase(java.util.Locale.US);
  }

  public static String toSlug(String value) {
    String normalized = normalizeText(value);
    if (!StringUtils.hasText(normalized)) {
      return null;
    }
    return normalized.replace(' ', '-');
  }

  public static Set<String> normalizePreferredGenres(List<String> preferredGenres) {
    if (CollectionUtils.isEmpty(preferredGenres)) {
      return Collections.emptySet();
    }
    return preferredGenres.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(TextNormalizer::toSlug)
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
  }
}
