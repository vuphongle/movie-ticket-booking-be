package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.Seat;
import vn.edu.iuh.fit.entity.SeatReservation;
import vn.edu.iuh.fit.entity.Showtime;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import vn.edu.iuh.fit.model.request.SeatReservationRequest;
import vn.edu.iuh.fit.model.response.SeatReservationResponse;
import vn.edu.iuh.fit.repository.SeatRepository;
import vn.edu.iuh.fit.repository.SeatReservationRepository;
import vn.edu.iuh.fit.repository.ShowtimeRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    private final SeatReservationRepository seatReservationRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void reserveSeat(SeatReservationRequest request) {
        // Giả định phương thức này kiểm tra sự tồn tại và khả dụng của ghế và suất chiếu
        // Nếu ghế không khả dụng, ném một ngoại lệ
        if (seatReservationRepository.existsBySeat_IdAndShowtime_IdAndStatusIn(
                request.getSeatId(), request.getShowtimeId(), List.of(SeatReservationStatus.HELD, SeatReservationStatus.BOOKED))) {
            throw new BadRequestException("Ghế đã được đặt hoặc giữ bởi người khác");
        }

        // Get the seat and showtime from the request
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghế"));

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));

        SeatReservation reservation = SeatReservation.builder()
                .seat(seat)
                .showtime(showtime)
                .status(SeatReservationStatus.HELD)
                .startTime(LocalDateTime.now())
                .build();
        seatReservationRepository.save(reservation);

        SeatReservationResponse response = SeatReservationResponse.builder()
                .seatId(reservation.getSeat().getId())
                .showtimeId(reservation.getShowtime().getId())
                .status(reservation.getStatus())
                .build();
        messagingTemplate.convertAndSend("/topic/seatUpdate", response);
    }

    public void cancelReservation(SeatReservationRequest request) {
        SeatReservation reservation = seatReservationRepository.findBySeat_IdAndShowtime_IdAndStatus(request.getSeatId(), request.getShowtimeId(), SeatReservationStatus.HELD)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        seatReservationRepository.delete(reservation);

        SeatReservationResponse response = SeatReservationResponse.builder()
                .seatId(request.getSeatId())
                .showtimeId(request.getShowtimeId())
                .status(null)
                .build();
        messagingTemplate.convertAndSend("/topic/seatUpdate", response);
    }
}