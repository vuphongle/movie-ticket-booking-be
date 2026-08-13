package vn.edu.iuh.fit.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.model.enums.OrderStatus;
import vn.edu.iuh.fit.repository.custom.OrderRepositoryCustom;

public interface OrderRepository extends JpaRepository<Order, Integer>, OrderRepositoryCustom {
  List<vn.edu.iuh.fit.entity.Order> findByUser_Id(Integer userId);

  List<Order> findByStatusAndCreatedAtBefore(OrderStatus orderStatus, LocalDateTime createdAt);

  List<Order> findByUser_IdAndStatusOrderByCreatedAtDesc(Integer id, OrderStatus status);

  List<Order> findByUser_IdOrderByCreatedAtDesc(Integer userId);

  boolean existsByShowtime_Id(Integer showtimeId);

  boolean existsByShowtime_IdAndStatusIn(Integer showtimeId, Collection<OrderStatus> statuses);
}
