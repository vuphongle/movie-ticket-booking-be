package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<vn.edu.iuh.fit.entity.Order> findByUser_Id(Integer userId);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus orderStatus, LocalDateTime createdAt);

    List<Order> findByUser_IdAndStatusOrderByCreatedAtDesc(Integer id, OrderStatus status);

    List<Order> findByUser_IdOrderByCreatedAtDesc(Integer userId);
}