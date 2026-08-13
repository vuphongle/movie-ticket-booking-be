package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "seat_reservations")
public class SeatReservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @ManyToOne private Seat seat; // thông tin ghế

  @ManyToOne private Showtime showtime; // thông tin suất chiếu

  @Enumerated(EnumType.STRING)
  private SeatReservationStatus status; // HELD, BOOKED, CANCELLED

  private LocalDateTime startTime; // thời gian bắt đầu đặt ghế

  @PrePersist
  protected void onCreate() {
    startTime = LocalDateTime.now();
  }
}
