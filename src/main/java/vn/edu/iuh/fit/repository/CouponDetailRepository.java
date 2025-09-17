package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.CouponDetail;

import java.util.List;

@Repository
public interface CouponDetailRepository extends JpaRepository<CouponDetail, Integer> {
    
    @Query("SELECT cd FROM CouponDetail cd WHERE cd.coupon.id = :couponId ORDER BY cd.linePriority ASC")
    List<CouponDetail> findByCouponIdOrderByLinePriorityAsc(@Param("couponId") Integer couponId);
    
    @Query("SELECT cd FROM CouponDetail cd WHERE cd.coupon.id = :couponId AND cd.enabled = true ORDER BY cd.linePriority ASC")
    List<CouponDetail> findByCouponIdAndEnabledTrueOrderByLinePriorityAsc(@Param("couponId") Integer couponId);
    
    @Query("SELECT COUNT(cd) FROM CouponDetail cd WHERE cd.coupon.id = :couponId AND cd.enabled = true")
    Long countEnabledDetailsByCouponId(@Param("couponId") Integer couponId);
    
    @Query("SELECT COUNT(cd) FROM CouponDetail cd WHERE cd.coupon.id = :couponId")
    Long countTotalDetailsByCouponId(@Param("couponId") Integer couponId);
    
    @Query("SELECT COALESCE(SUM(cd.detailUsedCount), 0) FROM CouponDetail cd WHERE cd.coupon.id = :couponId")
    Long sumUsedCountByCouponId(@Param("couponId") Integer couponId);
    
    @Query("DELETE FROM CouponDetail cd WHERE cd.coupon.id = :couponId")
    void deleteByCouponId(@Param("couponId") Integer couponId);
}