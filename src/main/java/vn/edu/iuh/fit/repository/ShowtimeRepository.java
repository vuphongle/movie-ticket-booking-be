package vn.edu.iuh.fit.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Showtime;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {
    List<Showtime> findAll(Specification<Showtime> showtimeSpecification, Sort sort);

    List<Showtime> findByAuditorium_IdAndDate(Integer id, LocalDate date);

    List<Showtime> findByMovie_IdAndDate(Integer id, LocalDate date);

    boolean existsByMovie_IdAndDateBetween(Integer id, LocalDate currentDate, LocalDate endDate);

    boolean existsByMovie_Id(Integer id);
}
