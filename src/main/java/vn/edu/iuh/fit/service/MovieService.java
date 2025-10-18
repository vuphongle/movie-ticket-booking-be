package vn.edu.iuh.fit.service;

import com.github.slugify.Slugify;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertMovieRequest;
import vn.edu.iuh.fit.repository.*;
import vn.edu.iuh.fit.utils.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
  private final MovieRepository movieRepository;
  private final ScheduleRepository scheduleRepository;
  private final GenreRepository genreRepository;
  private final DirectorRepository directorRepository;
  private final ActorRepository actorRepository;
  private final CountryRepository countryRepository;
  private final Slugify slugify;

  public List<Movie> getShowingNowMovies() {
    log.info("Get showing now movies");
    List<Schedule> schedules =
        scheduleRepository.findByMovie_StatusAndStartDateBeforeAndEndDateAfter(
            true, new Date(), new Date());
    return schedules.stream().map(Schedule::getMovie).toList();
  }

  public List<Movie> getComingSoonMovies() {
    log.info("Get coming soon movies");
    List<Schedule> schedules =
        scheduleRepository.findByMovie_StatusAndStartDateAfter(true, new Date());
    return schedules.stream().map(Schedule::getMovie).toList();
  }

  public Page<Movie> getAllReviewsOfMovies(Integer page, Integer limit) {
    Pageable pageable = PageRequest.of(page - 1, limit);
    Page<Movie> pageData = movieRepository.findByStatus(true, pageable);

    pageData
        .getContent()
        .forEach(
            movie -> {
              Set<Review> reviews = movie.getReviews();
              List<Review> sortedReviews =
                  reviews.stream()
                      .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                      .toList();
              movie.setReviews(new LinkedHashSet<>(sortedReviews));
            });

    return pageData;
  }

  public Movie getMovieDetail(Integer id, String slug) {
    log.info("Get movie detail by id = {}", id);
    return movieRepository
        .findByIdAndSlugAndStatus(id, slug, true)
        .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
  }

  public List<Movie> getAllMoviesInSchedule(String dateStr) {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    Date date = null;
    try {
      date = sdf.parse(dateStr);
    } catch (ParseException e) {
      throw new RuntimeException(e.getMessage());
    }
    // Lấy danh sách movie trong schedule đang có lịch chiếu hoặc sắp chiếu
    List<Schedule> schedules = scheduleRepository.findByMovie_StatusAndEndDateAfter(true, date);
    return schedules.stream().map(Schedule::getMovie).toList();
  }

  public List<Movie> getAllMovies(Boolean status) {
    log.info("Get all movies");
    if (status != null) {
      return movieRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    return movieRepository.findAll(Sort.by("createdAt").descending());
  }

  public Movie saveMovie(UpsertMovieRequest request) {
    Country country =
        countryRepository
            .findById(request.getCountryId())
            .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    List<Genre> genreList = genreRepository.findAllById(request.getGenreIds());
    List<Actor> actorList = actorRepository.findAllById(request.getActorIds());
    List<Director> directorList = directorRepository.findAllById(request.getDirectorIds());

    Movie movie =
        Movie.builder()
            .name(request.getName())
            .nameEn(request.getNameEn())
            .slug(slugify.slugify(request.getName()))
            .trailer(request.getTrailer())
            .description(request.getDescription())
            .poster(StringUtils.generateLinkImage(request.getName()))
            .releaseYear(request.getReleaseYear())
            .duration(request.getDuration())
            .status(request.getStatus())
            .showDate(request.getShowDate())
            .age(request.getAge())
            .country(country)
            .graphics(request.getGraphics())
            .translations(request.getTranslations())
            .genres(new HashSet<>(genreList))
            .actors(new HashSet<>(actorList))
            .directors(new HashSet<>(directorList))
            .build();
    return movieRepository.save(movie);
  }

  public Movie getMovieById(Integer id) {
    return movieRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
  }

  public Movie updateMovie(Integer id, UpsertMovieRequest request) {
    Movie movie =
        movieRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

    Country country =
        countryRepository
            .findById(request.getCountryId())
            .orElseThrow(() -> new ResourceNotFoundException("Country not found"));

    List<Genre> genreList = genreRepository.findAllById(request.getGenreIds());
    List<Actor> actorList = actorRepository.findAllById(request.getActorIds());
    List<Director> directorList = directorRepository.findAllById(request.getDirectorIds());

    movie.setName(request.getName());
    movie.setNameEn(request.getNameEn());
    movie.setSlug(slugify.slugify(request.getName()));
    movie.setTrailer(request.getTrailer());
    movie.setDescription(request.getDescription());
    movie.setPoster(request.getPoster());
    movie.setReleaseYear(request.getReleaseYear());
    movie.setDuration(request.getDuration());
    movie.setStatus(request.getStatus());
    movie.setShowDate(request.getShowDate());
    movie.setAge(request.getAge());
    movie.setCountry(country);
    movie.setGraphics(request.getGraphics());
    movie.setTranslations(request.getTranslations());
    movie.setGenres(new HashSet<>(genreList));
    movie.setActors(new HashSet<>(actorList));
    movie.setDirectors(new HashSet<>(directorList));
    return movieRepository.save(movie);
  }

  public void deleteMovie(Integer id) {
    movieRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    movieRepository.deleteById(id);
  }

  public List<Movie> searchShowingOrComingMovies(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return List.of();
    }
    return movieRepository.searchMovies(keyword.trim());
  }
}
