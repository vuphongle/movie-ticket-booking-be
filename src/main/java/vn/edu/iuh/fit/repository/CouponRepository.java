package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.model.enums.CouponStatus;
import vn.edu.iuh.fit.model.enums.CouponType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer> {
    
    // Basic finder by unique code
    Optional<Coupon> findByCode(String code);
    
    // Check if code already exists (for validation)
    boolean existsByCode(String code);
    
    // Find by status
    List<Coupon> findByStatus(CouponStatus status);
    
    // Find by type
    List<Coupon> findByType(CouponType type);
    
    // Find active coupons (ACTIVE status and within date range)
    @Query("SELECT c FROM Coupon c WHERE c.status = :status " +
           "AND c.startAt <= :now AND c.endAt >= :now")
    List<Coupon> findActiveCoupons(@Param("status") CouponStatus status, 
                                   @Param("now") LocalDateTime now);
    
    // Find coupons by date range
    @Query("SELECT c FROM Coupon c WHERE c.startAt <= :endDate AND c.endAt >= :startDate")
    List<Coupon> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                 @Param("endDate") LocalDateTime endDate);
    
    // Find promotions (type = PROMOTION and visible = true)
    @Query("SELECT c FROM Coupon c WHERE c.type = 'PROMOTION' AND c.visible = true")
    List<Coupon> findVisiblePromotions();
    
    // Count coupons by status
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.status = :status")
    Long countByStatus(@Param("status") CouponStatus status);
}