package vn.edu.iuh.fit.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
  Page<Movie> findByStatus(boolean status, Pageable pageable);

  boolean existsByIdAndStatus(Integer id, Boolean status);

  Optional<Movie> findByIdAndSlugAndStatus(Integer id, String slug, boolean status);

  List<Movie> findByStatusOrderByCreatedAtDesc(Boolean status);

  long countByGenres_Id(Integer genreId);

  long countByCountry_Id(Integer countryId);

  long countByActors_Id(Integer actorId);

  long countByDirectors_Id(Integer directorId);

  @Query(
      value =
          """
        SELECT DISTINCT m.*
        FROM movies m
        JOIN schedules s ON s.movie_id = m.id
        WHERE m.status = true
            AND ( (s.start_date <= NOW() AND s.end_date >= NOW())
            OR s.start_date > NOW() )
            AND UPPER(m.name) LIKE CONCAT('%', UPPER(:keyword), '%')
    """,
      nativeQuery = true)
  List<Movie> searchMovies(@Param("keyword") String keyword);

  @Query(
      value =
          """
      SELECT DISTINCT m.*
      FROM movies m
      JOIN schedules s ON s.movie_id = m.id
      WHERE m.status = true
        AND ( (s.start_date <= NOW() AND s.end_date >= NOW())
              OR s.start_date > NOW() )
        AND (
          UPPER(m.name)       LIKE CONCAT('%', UPPER(:keyword), '%')
          OR UPPER(m.name_en) LIKE CONCAT('%', UPPER(:keyword), '%')
          OR UPPER(m.slug)    LIKE CONCAT('%', UPPER(:keyword), '%')
          OR UPPER(m.description) LIKE CONCAT('%', UPPER(:keyword), '%')
        )
    """,
      nativeQuery = true)
  List<Movie> searchMoviesAll(@Param("keyword") String keyword);
}
