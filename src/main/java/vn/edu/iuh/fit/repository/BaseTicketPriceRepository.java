package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.BaseTicketPrice;
import vn.edu.iuh.fit.model.enums.*;

import java.util.Optional;

public interface BaseTicketPriceRepository extends JpaRepository<BaseTicketPrice, Integer> {
    Optional<BaseTicketPrice> findBySeatTypeAndGraphicsTypeAndAuditoriumTypeAndScreeningTimeTypeAndDayType(SeatType seatType, GraphicsType graphicsType, AuditoriumType auditoriumType, ScreeningTimeType screeningTimeType, DayType dayType);
}