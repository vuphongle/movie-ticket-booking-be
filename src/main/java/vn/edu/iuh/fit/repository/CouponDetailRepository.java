package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.model.enums.CouponTargetType;
import vn.edu.iuh.fit.model.enums.CouponApplyScope;

import java.util.List;

@Repository
public interface CouponDetailRepository extends JpaRepository<CouponDetail, Integer> {
    
    // Find all details for a coupon
    List<CouponDetail> findByCouponId(Integer couponId);
    
    // Find by target type
    List<CouponDetail> findByTargetType(CouponTargetType targetType);
    
    // Find by coupon ID and target type
    List<CouponDetail> findByCouponIdAndTargetType(Integer couponId, CouponTargetType targetType);
    
    // Find details that target specific ref ID
    List<CouponDetail> findByTargetRefId(Integer targetRefId);
    
    // Find by apply scope
    List<CouponDetail> findByApplyScope(CouponApplyScope applyScope);
    
    // Get coupon ID by detail ID (useful for validation)
    @Query("SELECT cd.couponId FROM CouponDetail cd WHERE cd.id = :detailId")
    Integer findCouponIdByDetailId(@Param("detailId") Integer detailId);
    
    // Count details for a coupon
    @Query("SELECT COUNT(cd) FROM CouponDetail cd WHERE cd.couponId = :couponId")
    Long countByCouponId(@Param("couponId") Integer couponId);
    
    // Delete all details for a coupon (cascade operation)
    @Modifying
    @Transactional
    @Query("DELETE FROM CouponDetail cd WHERE cd.couponId = :couponId")
    void deleteByCouponId(@Param("couponId") Integer couponId);
    
    // Check if product is being used as gift in any coupon detail
    @Query("SELECT COUNT(cd) > 0 FROM CouponDetail cd " +
           "WHERE cd.giftServiceId = :productId")
    boolean existsByGiftServiceId(@Param("productId") Integer productId);
    
    // Find details with gift service ID
    List<CouponDetail> findByGiftServiceId(Integer giftServiceId);
}