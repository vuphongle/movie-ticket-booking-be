package vn.edu.iuh.fit.repository.custom;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.iuh.fit.model.dto.CouponPerformanceDto;
import vn.edu.iuh.fit.model.dto.CouponStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponTypeStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponUsageDto;

public interface CouponRepositoryCustom {

  /**
   * Get overall coupon statistics for dashboard
   *
   * @param startDate Start date for filtering
   * @param endDate End date for filtering
   * @return Overall statistics
   */
  CouponStatisticsDto getCouponStatistics(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Get performance statistics for all coupons
   *
   * @param startDate Start date for filtering
   * @param endDate End date for filtering
   * @return List of coupon performance
   */
  List<CouponPerformanceDto> getCouponPerformance(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Get performance statistics for a specific coupon
   *
   * @param couponId Coupon ID
   * @param startDate Start date for filtering
   * @param endDate End date for filtering
   * @return Coupon performance
   */
  CouponPerformanceDto getCouponPerformanceById(
      Integer couponId, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Get coupon usage history
   *
   * @param couponId Coupon ID (optional)
   * @param startDate Start date for filtering
   * @param endDate End date for filtering
   * @return List of usage records
   */
  List<CouponUsageDto> getCouponUsageHistory(
      Integer couponId, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Get statistics by coupon type (benefit type and target type)
   *
   * @param startDate Start date for filtering
   * @param endDate End date for filtering
   * @return List of type statistics
   */
  List<CouponTypeStatisticsDto> getCouponTypeStatistics(
      LocalDateTime startDate, LocalDateTime endDate);
}
