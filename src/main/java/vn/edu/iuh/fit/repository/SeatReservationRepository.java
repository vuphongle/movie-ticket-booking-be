package vn.edu.iuh.fit.repository;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import vn.edu.iuh.fit.entity.SeatReservation;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Integer> {
  List<SeatReservation> findByShowtime_Id(Integer showtimeId);

  boolean existsBySeat_IdAndShowtime_IdAndStatusIn(
      Integer seatId, Integer showtimeId, List<SeatReservationStatus> status);

  boolean existsByShowtime_IdAndStatusIn(Integer showtimeId, List<SeatReservationStatus> status);

  List<SeatReservation> findByShowtime_IdAndSeat_IdInAndStatus(
      Integer showtime_id, Collection<Integer> seat_id, SeatReservationStatus status);

  Optional<SeatReservation> findBySeat_IdAndShowtime_IdAndStatus(
      Integer seatId, Integer showtimeId, SeatReservationStatus seatReservationStatus);

  Optional<SeatReservation> findBySeat_IdAndShowtime_Id(Integer seatId, Integer showtimeId);

  @Modifying
  @Transactional
  int deleteByStatusAndStartTimeBefore(SeatReservationStatus status, LocalDateTime time);
}
