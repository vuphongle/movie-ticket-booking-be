package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.CouponDetailTerms;

@Repository
public interface CouponDetailTermsRepository extends JpaRepository<CouponDetailTerms, Integer> {
    
    @Query("SELECT terms FROM CouponDetailTerms terms WHERE terms.couponDetail.id = :couponDetailId")
    CouponDetailTerms findByCouponDetailId(@Param("couponDetailId") Integer couponDetailId);
    
    @Query("SELECT COALESCE(SUM(terms.detailUsedCount), 0) FROM CouponDetailTerms terms " +
           "JOIN terms.couponDetail cd " +
           "WHERE cd.coupon.id = :couponId")
    Long sumUsedCountByCouponId(@Param("couponId") Integer couponId);
    
    // Check if product is being used as gift in active coupon details
    @Query("SELECT COUNT(terms) > 0 FROM CouponDetailTerms terms " +
           "JOIN terms.couponDetail cd " +
           "WHERE terms.giftServiceId = :productId " +
           "AND cd.enabled = true " +
           "AND cd.coupon.status = true")
    boolean existsByGiftServiceIdAndEnabledTrueAndCouponStatusTrue(@Param("productId") Integer productId);
}