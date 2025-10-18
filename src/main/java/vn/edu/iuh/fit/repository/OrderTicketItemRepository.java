package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.OrderTicketItem;

public interface OrderTicketItemRepository extends JpaRepository<OrderTicketItem, Integer> {}
