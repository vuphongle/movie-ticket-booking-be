package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.*;

/**
 * @deprecated This entity is deprecated. Use PriceList/PriceItem with targetType=TICKET instead.
 * Will be removed in future versions.
 */
@Deprecated
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "base_ticket_prices")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseTicketPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Enumerated(EnumType.STRING)
    SeatType seatType; // Loại ghế NORMAL, VIP, COUPLE

    @Enumerated(EnumType.STRING)
    GraphicsType graphicsType; // Loại phim 2D, 3D

    @Enumerated(EnumType.STRING)
    ScreeningTimeType screeningTimeType; // Loại suất chiếu: SUAT_CHIEU_SOM, SUAT_CHIEU_THEO_LICH

    @Enumerated(EnumType.STRING)
    DayType dayType; // Loại ngày áp dụng giá vé WEEKDAY, WEEKEND

    @Enumerated(EnumType.STRING)
    AuditoriumType auditoriumType; // Loại phòng chiếu STANDARD, IMAX, GOLDCLASS

    Integer price; // Giá vé
}
