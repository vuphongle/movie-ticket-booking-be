package vn.edu.iuh.fit.controller;

import vn.edu.iuh.fit.model.request.CancelMultipleSeatsRequest;
import vn.edu.iuh.fit.model.request.SeatReservationRequest;
import vn.edu.iuh.fit.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping("/seat-reservations/book")
    public ResponseEntity<?> bookSeat(@Valid @RequestBody SeatReservationRequest request) {
        reservationService.reserveSeat(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/seat-reservations/cancel")
    public ResponseEntity<?> cancelReservation(@Valid @RequestBody SeatReservationRequest request) {
        reservationService.cancelReservation(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/seat-reservations/cancel-multiple")
    public ResponseEntity<?> cancelMultipleReservations(@Valid @RequestBody CancelMultipleSeatsRequest request) {
        reservationService.cancelMultipleReservations(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}