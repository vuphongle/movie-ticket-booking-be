package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Showtime;

import java.time.LocalDate;
import java.util.List;

public interface ShowTimeRepository extends JpaRepository<Showtime, Integer> {
    List<Showtime> findByMovie_IdAndDate(Integer id, LocalDate date);
    boolean existsByMovie_IdAndDateBetween(Integer id, LocalDate currentDate, LocalDate endDate);
}
