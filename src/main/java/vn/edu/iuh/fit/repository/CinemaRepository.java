package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Cinema;

public interface CinemaRepository extends JpaRepository<Cinema, Integer> {
}
