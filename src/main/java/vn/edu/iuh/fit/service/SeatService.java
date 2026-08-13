package vn.edu.iuh.fit.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.*;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.DayType;
import vn.edu.iuh.fit.model.enums.ScreeningTimeType;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import vn.edu.iuh.fit.model.request.UpdateRowSeatRequest;
import vn.edu.iuh.fit.model.request.UpsertSeatRequest;
import vn.edu.iuh.fit.model.response.SeatResponse;
import vn.edu.iuh.fit.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {
  private final AuditoriumRepository auditoriumRepository;
  private final SeatRepository seatRepository;
  private final SeatReservationRepository seatReservationRepository;
  private final PricingService pricingService; // Thay thế BaseTicketPriceRepository
  private final ShowtimeRepository showtimeRepository;
  private final ScheduleRepository scheduleRepository;

  public List<SeatResponse> getSeatsByAuditoriumAndShowtime(
      Integer auditoriumId, Integer showtimeId) {
    List<Seat> seats = seatRepository.findByAuditorium_Id(auditoriumId);
    Showtime showtime =
        showtimeRepository
            .findById(showtimeId)
            .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

    Date now = new Date();
    List<Schedule> schedules = scheduleRepository.findByMovie_Id(showtime.getMovie().getId());
    // Get schedule has start date <= now <= end date hoặc now gần nhất với start date
    Schedule schedule =
        schedules.stream()
            .filter(
                s ->
                    s.getStartDate().before(now) && s.getEndDate().after(now)
                        || s.getStartDate().after(now))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

    List<SeatReservation> reservations = seatReservationRepository.findByShowtime_Id(showtimeId);

    ScreeningTimeType screeningTimeType =
        now.before(schedule.getStartDate())
            ? ScreeningTimeType.SUAT_CHIEU_SOM
            : ScreeningTimeType.SUAT_CHIEU_THEO_LICH;

    DayType dayType =
        showtime.getDate().getDayOfWeek().getValue() < 6 ? DayType.WEEKDAY : DayType.WEEKEND;

    Map<Integer, SeatReservationStatus> seatStatusMap =
        reservations.stream()
            .collect(
                Collectors.toMap(
                    reservation -> reservation.getSeat().getId(), SeatReservation::getStatus));

    Auditorium auditorium =
        auditoriumRepository
            .findById(auditoriumId)
            .orElseThrow(() -> new RuntimeException("Auditorium not found"));

    return seats.stream()
        .map(
            seat -> {
              SeatResponse response =
                  SeatResponse.builder()
                      .id(seat.getId())
                      .rowIndex(seat.getRowIndex())
                      .colIndex(seat.getColIndex())
                      .code(seat.getCode())
                      .type(seat.getType())
                      .status(seat.getStatus())
                      .reservationStatus(seatStatusMap.getOrDefault(seat.getId(), null))
                      .build();

              // Lấy PriceItem thay vì chỉ giá
              Optional<PriceItem> priceItemOpt =
                  pricingService.getPriceItemForTicket(
                      seat.getType(),
                      showtime.getGraphicsType(),
                      screeningTimeType,
                      dayType,
                      auditorium.getType(),
                      new Date() // ngày hiện tại
                      );

              priceItemOpt.ifPresentOrElse(
                  pi -> {
                    response.setPrice(pi.getPrice());
                    response.setPriceId(pi.getId());
                  },
                  () -> {
                    response.setPrice(0);
                    response.setPriceId(null);
                  });
              return response;
            })
        .collect(Collectors.toList());
  }

  public Seat updateSeat(Integer id, UpsertSeatRequest request) {
    Seat seat =
        seatRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghế"));

    Auditorium auditorium =
        auditoriumRepository
            .findById(request.getAuditoriumId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chiếu"));

    seat.setAuditorium(auditorium);
    seat.setRowIndex(request.getRowIndex());
    seat.setColIndex(request.getColIndex());
    seat.setCode(request.getCode());
    seat.setType(request.getType());
    seat.setStatus(request.getStatus());

    return seatRepository.save(seat);
  }

  public void updateRowSeat(UpdateRowSeatRequest request) {
    // Get all seats in the row and auditorium
    List<Seat> seats =
        seatRepository.findByAuditorium_IdAndRowIndex(
            request.getAuditoriumId(), request.getRowIndex());

    // Update all seats in the row
    seats.forEach(
        seat -> {
          seat.setType(request.getType());
          seat.setStatus(request.getStatus());
          seatRepository.save(seat);
        });
  }

  public SeatReservationStatus checkSeatReservationStatus(Integer seatId, Integer showtimeId) {
    // Tìm reservation tương ứng với ghế và suất chiếu
    Optional<SeatReservation> reservationOpt =
        seatReservationRepository.findBySeat_IdAndShowtime_Id(seatId, showtimeId);

    if (reservationOpt.isEmpty()) {
      return null; // Chưa được giữ hay đặt
    }

    return reservationOpt.get().getStatus();
  }
}
