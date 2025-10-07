package vn.edu.iuh.fit.service.chat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.entity.Movie;
import vn.edu.iuh.fit.model.enums.MovieAge;

@Component
public class AgeRestrictionService {

  private static final Map<MovieAge, Integer> AGE_MINIMUMS =
      Map.of(
          MovieAge.P, 0,
          MovieAge.K, 7,
          MovieAge.T13, 13,
          MovieAge.T16, 16,
          MovieAge.T18, 18,
          MovieAge.C, Integer.MAX_VALUE);

  public int determineMinimumAge(List<Integer> ages) {
    return ages.stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
  }

  public boolean isAgeAllowed(Movie movie, int groupMinAge) {
    if (movie == null || movie.getAge() == null) {
      return false;
    }
    return isAgeAllowed(movie.getAge(), groupMinAge);
  }

  public boolean isAgeAllowed(MovieAge age, int groupMinAge) {
    Integer requiredAge = AGE_MINIMUMS.get(age);
    if (requiredAge == null || requiredAge == Integer.MAX_VALUE) {
      return false;
    }
    return groupMinAge >= requiredAge;
  }

  public List<MovieAge> resolveAllowedRatings(int groupMinAge) {
    return AGE_MINIMUMS.entrySet().stream()
        .filter(entry -> entry.getKey() != MovieAge.C)
        .filter(entry -> groupMinAge >= entry.getValue())
        .sorted(Comparator.comparingInt(Map.Entry::getValue))
        .map(Map.Entry::getKey)
        .toList();
  }

  public Map<MovieAge, Integer> getAgeMinimums() {
    return Collections.unmodifiableMap(AGE_MINIMUMS);
  }
}
