package vn.edu.iuh.fit.service.chat;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KeywordAnalyzer {

  private final Map<String, KeywordConfig> keywordConfig;

  public KeywordAnalyzer() {
    this.keywordConfig = buildKeywordConfig();
  }

  public KeywordContext analyze(String message) {
    if (!StringUtils.hasText(message)) {
      return KeywordContext.empty();
    }
    String normalizedSlugMessage = TextNormalizer.toSlug(message);
    String normalizedTextMessage = TextNormalizer.normalizeText(message);
    if (!StringUtils.hasText(normalizedSlugMessage)
        && !StringUtils.hasText(normalizedTextMessage)) {
      return KeywordContext.empty();
    }

    Set<String> genreSlugs = new LinkedHashSet<>();
    Set<String> namePatterns = new LinkedHashSet<>();

    keywordConfig.forEach(
        (key, config) -> {
          boolean matched = false;
          if (StringUtils.hasText(normalizedSlugMessage) && normalizedSlugMessage.contains(key)) {
            matched = true;
          } else if (StringUtils.hasText(normalizedTextMessage)) {
            String keyAsText = key.replace('-', ' ');
            if (normalizedTextMessage.contains(keyAsText)) {
              matched = true;
            }
          }
          if (matched) {
            genreSlugs.addAll(config.genreSlugs());
            namePatterns.addAll(config.namePatterns());
          }
        });

    return new KeywordContext(genreSlugs, namePatterns);
  }

  private Map<String, KeywordConfig> buildKeywordConfig() {
    Map<String, KeywordConfig> config = new HashMap<>();

    KeywordConfig dinosaurConfig =
        createKeywordConfig(
            Set.of("khoa-hoc", "phieu-luu", "hanh-dong", "gay-can"),
            Set.of("khung long", "dinosaur", "jurassic"));
    register(config, dinosaurConfig, "khung long", "dinosaur", "jurassic");

    KeywordConfig romanceConfig =
        createKeywordConfig(
            Set.of("lang-man", "romance"),
            Set.of("lang man", "tinh yeu", "romance", "romantic"));
    register(config, romanceConfig, "lang man", "tinh yeu", "romance");

    KeywordConfig horrorConfig =
        createKeywordConfig(
            Set.of("kinh-di", "gay-can", "horror"),
            Set.of("kinh di", "horror", "scary"));
    register(config, horrorConfig, "kinh di", "gay can", "horror");

    KeywordConfig comedyConfig =
        createKeywordConfig(
            Set.of("hai", "hai-huoc", "comedy"),
            Set.of("hai", "hai huoc", "comedy", "funny"));
    register(config, comedyConfig, "hai", "hai huoc", "comedy", "funny");

    KeywordConfig romComConfig =
        createKeywordConfig(
            Set.of("lang-man", "hai", "comedy"),
            Set.of("rom com", "rom-com", "romantic comedy"));
    register(config, romComConfig, "rom com", "rom-com", "romantic comedy");

    KeywordConfig animationConfig =
        createKeywordConfig(
            Set.of("hoat-hinh", "animation"),
            Set.of("hoat hinh", "animation", "animated"));
    register(config, animationConfig, "hoat hinh", "animation", "animated");

    return Collections.unmodifiableMap(config);
  }

  private KeywordConfig createKeywordConfig(Set<String> genreSlugs, Set<String> namePatterns) {
    Set<String> normalizedGenres =
        genreSlugs == null
            ? Collections.emptySet()
            : genreSlugs.stream()
                .map(TextNormalizer::toSlug)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

    Set<String> normalizedNamePatterns =
        namePatterns == null
            ? Collections.emptySet()
            : namePatterns.stream()
                .map(TextNormalizer::normalizeText)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

    return new KeywordConfig(normalizedGenres, normalizedNamePatterns);
  }

  private void register(
      Map<String, KeywordConfig> config, KeywordConfig keywordConfig, String... keys) {
    for (String key : keys) {
      String slug = TextNormalizer.toSlug(key);
      if (StringUtils.hasText(slug)) {
        config.put(slug, keywordConfig);
      }
    }
  }

  public record KeywordContext(Set<String> genreSlugs, Set<String> namePatterns) {
    public static KeywordContext empty() {
      return new KeywordContext(Collections.emptySet(), Collections.emptySet());
    }
  }

  private record KeywordConfig(Set<String> genreSlugs, Set<String> namePatterns) {}
}
