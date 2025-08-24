package vn.edu.iuh.fit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Movie;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Page<Movie> findByStatus(boolean status, Pageable pageable);

    boolean existsByIdAndStatus(Integer id, Boolean status);
}
