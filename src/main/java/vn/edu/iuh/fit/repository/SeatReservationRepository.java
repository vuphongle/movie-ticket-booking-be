package vn.edu.iuh.fit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.SeatReservation;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Integer> {
  List<SeatReservation> findByShowtime_Id(Integer showtimeId);

  boolean existsBySeat_IdAndShowtime_IdAndStatusIn(
      Integer seatId, Integer showtimeId, List<SeatReservationStatus> status);

  List<SeatReservation> findByStatusAndStartTimeBefore(
      SeatReservationStatus seatReservationStatus, LocalDateTime localDateTime);

  Optional<SeatReservation> findBySeat_IdAndShowtime_IdAndStatus(
      Integer seatId, Integer showtimeId, SeatReservationStatus seatReservationStatus);

  Optional<SeatReservation> findBySeat_IdAndShowtime_Id(Integer seatId, Integer showtimeId);
}
