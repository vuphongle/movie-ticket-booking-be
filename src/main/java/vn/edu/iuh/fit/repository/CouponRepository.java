package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {
    boolean existsByCode(String code);
}