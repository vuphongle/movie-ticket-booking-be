package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.SeatReservation;
import java.util.List;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Integer> {
    List<SeatReservation> findByShowtime_Id(Integer showtimeId);
}
