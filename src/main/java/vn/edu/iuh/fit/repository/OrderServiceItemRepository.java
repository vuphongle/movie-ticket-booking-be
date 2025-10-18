package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.OrderServiceItem;

public interface OrderServiceItemRepository extends JpaRepository<OrderServiceItem, Integer> {}
