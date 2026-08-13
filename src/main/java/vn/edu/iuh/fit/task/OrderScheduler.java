package vn.edu.iuh.fit.task;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.model.enums.OrderStatus;
import vn.edu.iuh.fit.model.enums.SeatReservationStatus;
import vn.edu.iuh.fit.repository.OrderRepository;
import vn.edu.iuh.fit.repository.SeatReservationRepository;
import vn.edu.iuh.fit.service.OrderService;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

  private final OrderRepository orderRepository;
  private final SeatReservationRepository seatReservationRepository; // ✅ inject
  private final OrderService orderService;

  // Chạy mỗi 30s
  @Scheduled(fixedRate = 30000)
  public void autoCancelExpiredOrders() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime eightMinutesAgo = now.minusMinutes(8);

    // 1) Hủy đơn PENDING quá 8 phút
    List<Order> expiredOrders =
        orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, eightMinutesAgo);

    for (Order order : expiredOrders) {
      try {
        log.info("Đơn #{} quá hạn thanh toán, tiến hành hủy...", order.getId());
        orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED);
        log.info("Đơn #{} đã được hủy thành công", order.getId());
      } catch (Exception e) {
        log.error("Lỗi khi hủy đơn #{}: {}", order.getId(), e.getMessage());
      }
    }

    // 2) Giải phóng ghế HELD quá 8 phút
    try {
      int deleted =
          seatReservationRepository.deleteByStatusAndStartTimeBefore(
              SeatReservationStatus.HELD, eightMinutesAgo);

      if (deleted > 0) {
        log.info("Đã giải phóng {} ghế HELD quá 8 phút (trước {})", deleted, eightMinutesAgo);
      }
    } catch (Exception e) {
      log.error("Lỗi khi giải phóng ghế HELD quá hạn: {}", e.getMessage(), e);
    }
  }
}
