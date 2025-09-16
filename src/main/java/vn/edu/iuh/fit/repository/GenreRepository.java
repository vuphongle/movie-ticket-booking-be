package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Genre;


public interface GenreRepository extends JpaRepository<Genre, Integer> {
}