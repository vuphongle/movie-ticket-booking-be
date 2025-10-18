package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.SeatReservation;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Integer> {
    List<SeatReservation> findByShowtime_Id(Integer showtimeId);

    boolean existsBySeat_IdAndShowtime_IdAndStatusIn(Integer seatId, Integer showtimeId, List<SeatReservationStatus> status);

    List<SeatReservation> findByStatusAndStartTimeBefore(SeatReservationStatus seatReservationStatus, LocalDateTime localDateTime);

    List<SeatReservation> findByShowtime_IdAndSeat_IdInAndStatus(Integer showtime_id, Collection<Integer> seat_id, SeatReservationStatus status);

    Optional<SeatReservation> findBySeat_IdAndShowtime_IdAndStatus(Integer seatId, Integer showtimeId, SeatReservationStatus seatReservationStatus);

    Optional<SeatReservation> findBySeat_IdAndShowtime_Id(Integer seatId, Integer showtimeId);
}
