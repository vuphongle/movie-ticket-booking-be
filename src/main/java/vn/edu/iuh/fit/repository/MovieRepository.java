package vn.edu.iuh.fit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Movie;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
    Page<Movie> findByStatus(boolean status, Pageable pageable);

    boolean existsByIdAndStatus(Integer id, Boolean status);

    Optional<Movie> findByIdAndSlugAndStatus(Integer id, String slug, boolean status);
}
