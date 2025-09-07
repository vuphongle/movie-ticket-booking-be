package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.DayType;
import vn.edu.iuh.fit.model.enums.ScreeningTimeType;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import vn.edu.iuh.fit.model.response.SeatResponse;
import vn.edu.iuh.fit.repository.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {
    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final BaseTicketPriceRepository baseTicketPriceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ScheduleRepository scheduleRepository;


    public List<SeatResponse> getSeatsByAuditoriumAndShowtime(Integer auditoriumId, Integer showtimeId) {
        List<Seat> seats = seatRepository.findByAuditorium_Id(auditoriumId);
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        Date now = new Date();
        List<Schedule> schedules = scheduleRepository.findByMovie_Id(showtime.getMovie().getId());
        // Get schedule has start date <= now <= end date hoặc now gần nhất với start date
        Schedule schedule = schedules.stream()
                .filter(s -> s.getStartDate().before(now) && s.getEndDate().after(now) || s.getStartDate().after(now))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        List<SeatReservation> reservations = seatReservationRepository.findByShowtime_Id(showtimeId);

        ScreeningTimeType screeningTimeType = now.before(schedule.getStartDate()) ?
                ScreeningTimeType.SUAT_CHIEU_SOM : ScreeningTimeType.SUAT_CHIEU_THEO_LICH;

        DayType dayType = showtime.getDate().getDayOfWeek().getValue() < 6 ? DayType.WEEKDAY : DayType.WEEKEND;

        List<BaseTicketPrice> baseTicketPrices = baseTicketPriceRepository.findAll();

        Map<Integer, SeatReservationStatus> seatStatusMap = reservations.stream()
                .collect(Collectors.toMap(reservation -> reservation.getSeat().getId(), SeatReservation::getStatus));

        Auditorium auditorium = auditoriumRepository.findById(auditoriumId)
                .orElseThrow(() -> new RuntimeException("Auditorium not found"));

        return seats.stream().map(seat -> {
            SeatResponse response = SeatResponse.builder()
                    .id(seat.getId())
                    .rowIndex(seat.getRowIndex())
                    .colIndex(seat.getColIndex())
                    .code(seat.getCode())
                    .type(seat.getType())
                    .status(seat.getStatus())
                    .reservationStatus(seatStatusMap.getOrDefault(seat.getId(), null))
                    .build();

            Optional<BaseTicketPrice> matchedPrice = baseTicketPrices.stream().filter(price ->
                    price.getSeatType() == seat.getType() &&
                            price.getGraphicsType() == showtime.getGraphicsType() &&
                            price.getScreeningTimeType() == screeningTimeType &&
                            price.getDayType() == dayType &&
                            price.getAuditoriumType() == auditorium.getType()
            ).findFirst();

            response.setPrice(matchedPrice.map(BaseTicketPrice::getPrice).orElse(70000)); // Mặc định giá nếu không tìm thấy
            return response;
        }).collect(Collectors.toList());
    }
}
