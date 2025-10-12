package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.CouponDetail;

import java.util.List;

@Repository
public interface CouponDetailRepository extends JpaRepository<CouponDetail, Integer> {
    
    @Query("SELECT cd FROM CouponDetail cd WHERE cd.coupon.id = :couponId ORDER BY cd.id ASC")
    List<CouponDetail> findByCouponIdOrderByIdAsc(@Param("couponId") Integer couponId);
    
    @Query("SELECT cd FROM CouponDetail cd WHERE cd.coupon.id = :couponId AND cd.enabled = true ORDER BY cd.id ASC")
    List<CouponDetail> findByCouponIdAndEnabledTrueOrderByIdAsc(@Param("couponId") Integer couponId);

    @Query("""
    SELECT cd 
    FROM CouponDetail cd 
    WHERE cd.coupon.kind = 'DISPLAY' 
      AND cd.enabled = true 
      AND cd.coupon.startDate <= CURRENT_TIMESTAMP 
      AND cd.coupon.endDate >= CURRENT_TIMESTAMP 
    ORDER BY cd.id ASC
    """)
    List<CouponDetail> findAllValidDisplayCouponDetails();

    
    @Query("SELECT COUNT(cd) FROM CouponDetail cd WHERE cd.coupon.id = :couponId AND cd.enabled = true")
    Long countEnabledDetailsByCouponId(@Param("couponId") Integer couponId);
    
    @Query("SELECT COUNT(cd) FROM CouponDetail cd WHERE cd.coupon.id = :couponId")
    Long countTotalDetailsByCouponId(@Param("couponId") Integer couponId);
    
    @Query("SELECT cd.coupon.id FROM CouponDetail cd WHERE cd.id = :detailId")
    Integer findCouponIdByDetailId(@Param("detailId") Integer detailId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CouponDetail cd WHERE cd.coupon.id = :couponId")
    void deleteByCouponId(@Param("couponId") Integer couponId);
}