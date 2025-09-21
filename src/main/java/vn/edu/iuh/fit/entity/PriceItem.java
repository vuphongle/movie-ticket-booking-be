package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.*;

import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "price_items")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    PriceList priceList;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TargetType targetType; // PRODUCT, ADDITIONAL_SERVICE, TICKET

    Integer targetId; // nullable khi targetType = TICKET

    // Các trường dành cho vé (khi targetType = TICKET)
    @Enumerated(EnumType.STRING)
    SeatType seatType; // nullable, null = wildcard

    @Enumerated(EnumType.STRING)
    GraphicsType graphicsType; // nullable, null = wildcard

    @Enumerated(EnumType.STRING)
    ScreeningTimeType screeningTimeType; // nullable, null = wildcard

    @Enumerated(EnumType.STRING)
    DayType dayType; // nullable, null = wildcard

    @Enumerated(EnumType.STRING)
    AuditoriumType auditoriumType; // nullable, null = wildcard

    @Column(nullable = false)
    Integer price; // Giá áp dụng

    Integer minQty; // Số lượng tối thiểu để áp dụng giá này

    @Column(nullable = false)
    Integer priority; // Độ ưu tiên (số cao hơn = ưu tiên cao hơn)

    @Builder.Default
    Boolean status = true; // Trạng thái kích hoạt

    // Helper method để kiểm tra price item có hiệu lực tại thời điểm hiện tại không
    public boolean isValidAt(Date checkDate) {
        if (!status) return false;
        // Sử dụng thời gian hiệu lực từ PriceList cha
        if (priceList.getValidFrom() != null && checkDate.before(priceList.getValidFrom())) return false;
        if (priceList.getValidTo() != null && checkDate.after(priceList.getValidTo())) return false;
        return true;
    }

    // Helper method để kiểm tra xem price item có match với điều kiện vé không
    public boolean matchesTicketConditions(SeatType seatType, GraphicsType graphicsType, 
                                         ScreeningTimeType screeningTimeType, DayType dayType, 
                                         AuditoriumType auditoriumType) {
        if (targetType != TargetType.TICKET) return false;
        
        return (this.seatType == null || this.seatType.equals(seatType)) &&
               (this.graphicsType == null || this.graphicsType.equals(graphicsType)) &&
               (this.screeningTimeType == null || this.screeningTimeType.equals(screeningTimeType)) &&
               (this.dayType == null || this.dayType.equals(dayType)) &&
               (this.auditoriumType == null || this.auditoriumType.equals(auditoriumType));
    }
}