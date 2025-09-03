package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Auditorium;

import java.util.List;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Integer> {
    List<Auditorium> findByCinema_Id(Integer cinemaId);

    List<Auditorium> findByCinemaId(Integer cinemaId);
}