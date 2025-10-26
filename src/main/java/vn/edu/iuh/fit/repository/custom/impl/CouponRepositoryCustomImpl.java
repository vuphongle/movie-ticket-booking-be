package vn.edu.iuh.fit.repository.custom.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.model.dto.CouponPerformanceDto;
import vn.edu.iuh.fit.model.dto.CouponStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponTypeStatisticsDto;
import vn.edu.iuh.fit.model.dto.CouponUsageDto;
import vn.edu.iuh.fit.repository.custom.CouponRepositoryCustom;

@Slf4j
@Repository
public class CouponRepositoryCustomImpl implements CouponRepositoryCustom {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public CouponStatisticsDto getCouponStatistics(LocalDateTime startDate, LocalDateTime endDate) {
    CouponStatisticsDto stats = new CouponStatisticsDto();
    LocalDateTime now = LocalDateTime.now();

    // Count coupons by status
    String countQuery =
        "SELECT "
            + "COUNT(*) as total, "
            + "SUM(CASE WHEN c.status = true AND c.start_date <= :now AND c.end_date >= :now THEN 1 ELSE 0 END) as active, "
            + "SUM(CASE WHEN c.status = true AND c.start_date > :now THEN 1 ELSE 0 END) as upcoming, "
            + "SUM(CASE WHEN c.end_date < :now THEN 1 ELSE 0 END) as expired, "
            + "SUM(CASE WHEN c.status = false THEN 1 ELSE 0 END) as hidden "
            + "FROM coupons c";

    Query query = entityManager.createNativeQuery(countQuery);
    query.setParameter("now", now);
    Object[] countResult = (Object[]) query.getSingleResult();

    stats.setTotalCoupons(((Number) countResult[0]).intValue());
    stats.setActiveCoupons(((Number) countResult[1]).intValue());
    stats.setUpcomingCoupons(((Number) countResult[2]).intValue());
    stats.setExpiredCoupons(((Number) countResult[3]).intValue());
    stats.setHiddenCoupons(((Number) countResult[4]).intValue());

    // Get order statistics
    String orderStatsQuery =
        "SELECT "
            + "COUNT(DISTINCT o.id) as total_orders, "
            + "COUNT(DISTINCT CASE WHEN o.discount > 0 THEN o.id END) as orders_with_coupons, "
            + "COUNT(DISTINCT CASE WHEN o.discount = 0 THEN o.id END) as orders_without_coupons, "
            + "COUNT(DISTINCT CASE WHEN o.discount > 0 THEN o.user_id END) as unique_customers, "
            + "SUM(CASE WHEN o.discount > 0 THEN o.discount ELSE 0 END) as total_discount, "
            + "SUM(CASE WHEN o.discount > 0 THEN 1 ELSE 0 END) as redemptions "
            + "FROM orders o "
            + "WHERE o.status = 'CONFIRMED' "
            + "AND o.created_at BETWEEN :startDate AND :endDate";

    Query orderQuery = entityManager.createNativeQuery(orderStatsQuery);
    orderQuery.setParameter("startDate", startDate);
    orderQuery.setParameter("endDate", endDate);
    Object[] orderResult = (Object[]) orderQuery.getSingleResult();

    int totalOrders = ((Number) orderResult[0]).intValue();
    int ordersWithCoupons = ((Number) orderResult[1]).intValue();
    int ordersWithoutCoupons = ((Number) orderResult[2]).intValue();
    int uniqueCustomers = orderResult[3] != null ? ((Number) orderResult[3]).intValue() : 0;
    int totalDiscount = orderResult[4] != null ? ((Number) orderResult[4]).intValue() : 0;
    int redemptions = orderResult[5] != null ? ((Number) orderResult[5]).intValue() : 0;

    stats.setOrdersWithCoupons(ordersWithCoupons);
    stats.setOrdersWithoutCoupons(ordersWithoutCoupons);
    stats.setUniqueCustomers(uniqueCustomers);
    stats.setTotalDiscountAmount(totalDiscount);
    stats.setTotalRedemptions(redemptions);

    // Calculate revenue metrics
    String revenueQuery =
        "SELECT "
            + "COALESCE(SUM((SELECT COALESCE(SUM(oti.price), 0) FROM order_ticket_items oti WHERE oti.order_id = o.id) + "
            + "              (SELECT COALESCE(SUM(osi.price * osi.quantity), 0) FROM order_service_items osi WHERE osi.order_id = o.id) + "
            + "              o.discount), 0) as revenue_before, "
            + "COALESCE(SUM((SELECT COALESCE(SUM(oti.price), 0) FROM order_ticket_items oti WHERE oti.order_id = o.id) + "
            + "              (SELECT COALESCE(SUM(osi.price * osi.quantity), 0) FROM order_service_items osi WHERE osi.order_id = o.id)), 0) as revenue_after "
            + "FROM orders o "
            + "WHERE o.status = 'CONFIRMED' AND o.discount > 0 "
            + "AND o.created_at BETWEEN :startDate AND :endDate";

    Query revQuery = entityManager.createNativeQuery(revenueQuery);
    revQuery.setParameter("startDate", startDate);
    revQuery.setParameter("endDate", endDate);
    Object[] revResult = (Object[]) revQuery.getSingleResult();

    int revenueBefore = revResult[0] != null ? ((Number) revResult[0]).intValue() : 0;
    int revenueAfter = revResult[1] != null ? ((Number) revResult[1]).intValue() : 0;

    stats.setTotalRevenueBeforeDiscount(revenueBefore);
    stats.setTotalRevenueAfterDiscount(revenueAfter);

    // Calculate averages and rates
    if (ordersWithCoupons > 0) {
      stats.setAverageDiscountPerOrder((double) totalDiscount / ordersWithCoupons);
      stats.setAverageOrderValueWithCoupon((double) revenueAfter / ordersWithCoupons);
    } else {
      stats.setAverageDiscountPerOrder(0.0);
      stats.setAverageOrderValueWithCoupon(0.0);
    }

    if (revenueBefore > 0) {
      stats.setAverageDiscountPercentage((double) totalDiscount / revenueBefore * 100);
      stats.setDiscountImpactRate((double) totalDiscount / revenueBefore);
    } else {
      stats.setAverageDiscountPercentage(0.0);
      stats.setDiscountImpactRate(0.0);
    }

    if (totalOrders > 0) {
      stats.setOrderConversionRate((double) ordersWithCoupons / totalOrders * 100);
    } else {
      stats.setOrderConversionRate(0.0);
    }

    // Calculate orders without coupons average
    if (ordersWithoutCoupons > 0) {
      String avgNoDiscountQuery =
          "SELECT AVG((SELECT COALESCE(SUM(oti.price), 0) FROM order_ticket_items oti WHERE oti.order_id = o.id) + "
              + "           (SELECT COALESCE(SUM(osi.price * osi.quantity), 0) FROM order_service_items osi WHERE osi.order_id = o.id)) "
              + "FROM orders o "
              + "WHERE o.status = 'CONFIRMED' AND o.discount = 0 "
              + "AND o.created_at BETWEEN :startDate AND :endDate";
      Query avgQuery = entityManager.createNativeQuery(avgNoDiscountQuery);
      avgQuery.setParameter("startDate", startDate);
      avgQuery.setParameter("endDate", endDate);
      Object avgResult = avgQuery.getSingleResult();
      stats.setAverageOrderValueWithoutCoupon(
          avgResult != null ? ((Number) avgResult).doubleValue() : 0.0);
    } else {
      stats.setAverageOrderValueWithoutCoupon(0.0);
    }

    // Calculate usage rate (coupons that have been used vs total coupons in the system)
    // This metric shows what percentage of all coupons (not just active) have ever been used
    String usedCouponsQuery =
        "SELECT COUNT(DISTINCT cd.coupon_id) "
            + "FROM coupon_detail_terms cdt "
            + "JOIN coupon_details cd ON cd.id = cdt.id "
            + "WHERE cdt.detail_used_count > 0";
    Query usedQuery = entityManager.createNativeQuery(usedCouponsQuery);
    int usedCoupons = ((Number) usedQuery.getSingleResult()).intValue();

    if (stats.getTotalCoupons() > 0) {
      stats.setCouponUsageRate((double) usedCoupons / stats.getTotalCoupons() * 100);
    } else {
      stats.setCouponUsageRate(0.0);
    }

    return stats;
  }

  @Override
  public List<CouponPerformanceDto> getCouponPerformance(
      LocalDateTime startDate, LocalDateTime endDate) {
    String query =
        "SELECT "
            + "c.id, c.code, c.name, c.description, c.kind, c.status, c.start_date, c.end_date, "
            + "(SELECT COUNT(*) FROM coupon_details cd WHERE cd.coupon_id = c.id AND cd.enabled = 1) as enabled_details, "
            + "(SELECT COUNT(*) FROM coupon_details cd WHERE cd.coupon_id = c.id) as total_details, "
            + "(SELECT SUM(cdt.detail_used_count) FROM coupon_detail_terms cdt "
            + " JOIN coupon_details cd ON cd.id = cdt.id WHERE cd.coupon_id = c.id) as usage_count, "
            + "(SELECT COUNT(DISTINCT cd.benefit_type) FROM coupon_details cd WHERE cd.coupon_id = c.id AND cd.benefit_type = 'DISCOUNT_PERCENT') as discount_percent_count, "
            + "(SELECT COUNT(DISTINCT cd.benefit_type) FROM coupon_details cd WHERE cd.coupon_id = c.id AND cd.benefit_type = 'DISCOUNT_AMOUNT') as discount_amount_count, "
            + "(SELECT COUNT(DISTINCT cd.benefit_type) FROM coupon_details cd WHERE cd.coupon_id = c.id AND cd.benefit_type = 'FREE_PRODUCT') as free_product_count "
            + "FROM coupons c "
            + "ORDER BY c.created_at DESC";

    Query q = entityManager.createNativeQuery(query);
    @SuppressWarnings("unchecked")
    List<Object[]> results = q.getResultList();

    List<CouponPerformanceDto> performances = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (Object[] row : results) {
      CouponPerformanceDto dto = new CouponPerformanceDto();
      dto.setCouponId(((Number) row[0]).intValue());
      dto.setCouponCode((String) row[1]);
      dto.setCouponName((String) row[2]);
      dto.setDescription((String) row[3]);
      dto.setKind((String) row[4]);
      dto.setStatus((Boolean) row[5]);
      dto.setStartDate(toLocalDateTime(row[6]));
      dto.setEndDate(toLocalDateTime(row[7]));
      dto.setEnabledDetailsCount(((Number) row[8]).intValue());
      dto.setTotalDetailsCount(((Number) row[9]).intValue());
      dto.setUsageCount(row[10] != null ? ((Number) row[10]).intValue() : 0);
      dto.setDiscountPercentCount(((Number) row[11]).intValue());
      dto.setDiscountAmountCount(((Number) row[12]).intValue());
      dto.setFreeProductCount(((Number) row[13]).intValue());

      // Calculate days
      dto.setDaysActive(ChronoUnit.DAYS.between(dto.getStartDate(), now));
      dto.setDaysRemaining(ChronoUnit.DAYS.between(now, dto.getEndDate()));

      // Set status label
      dto.setStatusLabel(
          getCouponStatusLabel(dto.getStatus(), dto.getStartDate(), dto.getEndDate(), now));

      // Get financial metrics for this coupon (would need order tracking)
      enrichWithFinancialMetrics(dto, startDate, endDate);

      performances.add(dto);
    }

    return performances;
  }

  @Override
  public CouponPerformanceDto getCouponPerformanceById(
      Integer couponId, LocalDateTime startDate, LocalDateTime endDate) {
    List<CouponPerformanceDto> all = getCouponPerformance(startDate, endDate);
    return all.stream().filter(dto -> dto.getCouponId().equals(couponId)).findFirst().orElse(null);
  }

  @Override
  public List<CouponUsageDto> getCouponUsageHistory(
      Integer couponId, LocalDateTime startDate, LocalDateTime endDate) {
    // This would require a coupon_redemptions table or order tracking
    // For now, return based on orders with discount
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append(
        "SELECT o.id, '', '', o.discount, "
            + "((SELECT COALESCE(SUM(oti.price), 0) FROM order_ticket_items oti WHERE oti.order_id = o.id) + "
            + " (SELECT COALESCE(SUM(osi.price * osi.quantity), 0) FROM order_service_items osi WHERE osi.order_id = o.id) + o.discount), "
            + "((SELECT COALESCE(SUM(oti.price), 0) FROM order_ticket_items oti WHERE oti.order_id = o.id) + "
            + " (SELECT COALESCE(SUM(osi.price * osi.quantity), 0) FROM order_service_items osi WHERE osi.order_id = o.id)), "
            + "u.name, u.email, o.created_at, "
            + "(SELECT m.name FROM movies m WHERE m.id = s.movie_id), "
            + "(SELECT ci.name FROM cinemas ci WHERE ci.id = (SELECT a.cinema_id FROM auditoriums a WHERE a.id = s.auditorium_id)) "
            + "FROM orders o "
            + "JOIN users u ON u.id = o.user_id "
            + "LEFT JOIN showtimes s ON s.id = o.show_id "
            + "WHERE o.status = 'CONFIRMED' AND o.discount > 0 "
            + "AND o.created_at BETWEEN :startDate AND :endDate ");

    if (couponId != null) {
      // Would need proper coupon tracking in orders table
      queryBuilder.append("ORDER BY o.created_at DESC");
    } else {
      queryBuilder.append("ORDER BY o.created_at DESC");
    }

    Query query = entityManager.createNativeQuery(queryBuilder.toString());
    query.setParameter("startDate", startDate);
    query.setParameter("endDate", endDate);

    @SuppressWarnings("unchecked")
    List<Object[]> results = query.getResultList();

    List<CouponUsageDto> usages = new ArrayList<>();
    for (Object[] row : results) {
      CouponUsageDto dto = new CouponUsageDto();
      dto.setOrderId(((Number) row[0]).intValue());
      dto.setCouponCode((String) row[1]);
      dto.setCouponName((String) row[2]);
      dto.setDiscountAmount(((Number) row[3]).intValue());
      dto.setOrderTotalBeforeDiscount(((Number) row[4]).intValue());
      dto.setOrderTotalAfterDiscount(((Number) row[5]).intValue());
      dto.setCustomerName((String) row[6]);
      dto.setCustomerEmail((String) row[7]);
      dto.setUsedAt(toLocalDateTime(row[8]));
      dto.setMovieName((String) row[9]);
      dto.setCinemaName((String) row[10]);
      usages.add(dto);
    }

    return usages;
  }

  @Override
  public List<CouponTypeStatisticsDto> getCouponTypeStatistics(
      LocalDateTime startDate, LocalDateTime endDate) {
    List<CouponTypeStatisticsDto> statistics = new ArrayList<>();

    // Statistics by benefit type
    String benefitQuery =
        "SELECT cd.benefit_type, COUNT(DISTINCT cd.id), "
            + "SUM(cdt.detail_used_count), 0, 0 "
            + "FROM coupon_details cd "
            + "JOIN coupon_detail_terms cdt ON cdt.id = cd.id "
            + "GROUP BY cd.benefit_type";

    Query bQuery = entityManager.createNativeQuery(benefitQuery);
    @SuppressWarnings("unchecked")
    List<Object[]> benefitResults = bQuery.getResultList();

    for (Object[] row : benefitResults) {
      CouponTypeStatisticsDto dto = new CouponTypeStatisticsDto();
      dto.setType((String) row[0]);
      dto.setCategory("benefit");
      dto.setCount(((Number) row[1]).intValue());
      dto.setUsageCount(((Number) row[2]).intValue());
      dto.setTotalDiscountValue(0); // Would need order tracking
      dto.setOrderCount(0); // Would need order tracking
      dto.setAverageDiscount(0.0);
      statistics.add(dto);
    }

    // Statistics by target type
    String targetQuery =
        "SELECT cd.target_type, COUNT(DISTINCT cd.id), "
            + "SUM(cdt.detail_used_count), 0, 0 "
            + "FROM coupon_details cd "
            + "JOIN coupon_detail_terms cdt ON cdt.id = cd.id "
            + "GROUP BY cd.target_type";

    Query tQuery = entityManager.createNativeQuery(targetQuery);
    @SuppressWarnings("unchecked")
    List<Object[]> targetResults = tQuery.getResultList();

    for (Object[] row : targetResults) {
      CouponTypeStatisticsDto dto = new CouponTypeStatisticsDto();
      dto.setType((String) row[0]);
      dto.setCategory("target");
      dto.setCount(((Number) row[1]).intValue());
      dto.setUsageCount(((Number) row[2]).intValue());
      dto.setTotalDiscountValue(0);
      dto.setOrderCount(0);
      dto.setAverageDiscount(0.0);
      statistics.add(dto);
    }

    return statistics;
  }

  private void enrichWithFinancialMetrics(
      CouponPerformanceDto dto, LocalDateTime startDate, LocalDateTime endDate) {
    // Try to infer coupon-related financial metrics by matching orders whose
    // requestSnapshot contains coupon detail ids. This is a pragmatic approach
    // because orders do not have direct coupon_id foreign key. We look for
    // occurrences of "detailId":<id> in the JSON snapshot (as produced by
    // CreateOrderRequest) and aggregate confirmed orders in the date range.
    try {
      // 1) Get coupon detail ids for this coupon
      String detailIdQuery = "SELECT id FROM coupon_details WHERE coupon_id = :couponId";
      Query dQuery = entityManager.createNativeQuery(detailIdQuery);
      dQuery.setParameter("couponId", dto.getCouponId());
      @SuppressWarnings("unchecked")
      List<Object> detailIdsRaw = dQuery.getResultList();

      if (detailIdsRaw == null || detailIdsRaw.isEmpty()) {
        // No details -> no orders linked
        dto.setUniqueCustomers(0);
        dto.setTotalDiscountValue(0);
        dto.setRevenueWithCoupon(0);
        dto.setRevenueBeforeDiscount(0);
        dto.setAverageDiscountPerOrder(0.0);
        dto.setAverageOrderValue(0.0);
        dto.setUsageRate(0.0);
        dto.setConversionRate(0.0);
        dto.setDiscountPercentage(0.0);
        dto.setLastUsedAt(null);
        return;
      }

      // Build WHERE clause matching request_snapshot JSON for any of the detail ids
      StringBuilder patternWhere = new StringBuilder();
      for (int i = 0; i < detailIdsRaw.size(); i++) {
        Number idNum = (Number) detailIdsRaw.get(i);
        Integer detailId = idNum.intValue();
        if (i > 0) patternWhere.append(" OR ");
        // match e.g. \"detailId\":123 or \"detailId\": 123
        patternWhere.append("o.request_snapshot LIKE '%\\\"detailId\\\":");
        patternWhere.append(detailId);
        patternWhere.append("%'");
      }

      String metricsQuery =
          "SELECT COUNT(DISTINCT o.id), COUNT(DISTINCT o.user_id), "
              + "SUM(CASE WHEN o.discount > 0 THEN o.discount ELSE 0 END), "
              + "SUM((SELECT COALESCE(SUM(oti.price),0) FROM order_ticket_items oti WHERE oti.order_id = o.id) + "
              + "    (SELECT COALESCE(SUM(osi.price * osi.quantity),0) FROM order_service_items osi WHERE osi.order_id = o.id) + o.discount), "
              + "SUM((SELECT COALESCE(SUM(oti.price),0) FROM order_ticket_items oti WHERE oti.order_id = o.id) + "
              + "    (SELECT COALESCE(SUM(osi.price * osi.quantity),0) FROM order_service_items osi WHERE osi.order_id = o.id)), "
              + "MAX(o.created_at) "
              + "FROM orders o "
              + "WHERE o.status = 'CONFIRMED' AND ("
              + patternWhere.toString()
              + ") "
              + "AND o.created_at BETWEEN :startDate AND :endDate";

      Query mQuery = entityManager.createNativeQuery(metricsQuery);
      mQuery.setParameter("startDate", startDate);
      mQuery.setParameter("endDate", endDate);
      Object[] mResult = (Object[]) mQuery.getSingleResult();

      int ordersCount = mResult[0] != null ? ((Number) mResult[0]).intValue() : 0;
      int uniqueCustomers = mResult[1] != null ? ((Number) mResult[1]).intValue() : 0;
      int totalDiscount = mResult[2] != null ? ((Number) mResult[2]).intValue() : 0;
      int revenueBeforeDiscount = mResult[3] != null ? ((Number) mResult[3]).intValue() : 0;
      int revenueAfterDiscount = mResult[4] != null ? ((Number) mResult[4]).intValue() : 0;
      Object lastUsedObj = mResult[5];

      dto.setUniqueCustomers(uniqueCustomers);
      dto.setTotalDiscountValue(totalDiscount);
      dto.setRevenueBeforeDiscount(revenueBeforeDiscount);
      dto.setRevenueWithCoupon(revenueAfterDiscount);
      dto.setAverageDiscountPerOrder(ordersCount > 0 ? (double) totalDiscount / ordersCount : 0.0);
      dto.setAverageOrderValue(ordersCount > 0 ? (double) revenueAfterDiscount / ordersCount : 0.0);

      // usageCount is maintained in coupon_detail_terms and already set earlier; compute usageRate
      int usageCount = dto.getUsageCount() != null ? dto.getUsageCount() : 0;
      int totalDetails = dto.getTotalDetailsCount() != null ? dto.getTotalDetailsCount() : 0;
      dto.setUsageRate(totalDetails > 0 ? (double) usageCount / totalDetails * 100 : 0.0);

      // conversion rate: orders with this coupon / total orders in period
      // total orders in period (confirmed)
      String totalOrdersQuery =
          "SELECT COUNT(DISTINCT o.id) FROM orders o WHERE o.status = 'CONFIRMED' AND o.created_at BETWEEN :startDate AND :endDate";
      Query toQuery = entityManager.createNativeQuery(totalOrdersQuery);
      toQuery.setParameter("startDate", startDate);
      toQuery.setParameter("endDate", endDate);
      Number totOrdersNum = (Number) toQuery.getSingleResult();
      int totOrders = totOrdersNum != null ? totOrdersNum.intValue() : 0;
      dto.setConversionRate(totOrders > 0 ? (double) ordersCount / totOrders * 100 : 0.0);

      dto.setDiscountPercentage(
          revenueBeforeDiscount > 0 ? (double) totalDiscount / revenueBeforeDiscount * 100 : 0.0);

      dto.setLastUsedAt(toLocalDateTime(lastUsedObj));
    } catch (Exception e) {
      log.error("Error enriching coupon {} metrics: {}", dto.getCouponId(), e.getMessage());
      dto.setUniqueCustomers(0);
      dto.setTotalDiscountValue(0);
      dto.setRevenueWithCoupon(0);
      dto.setRevenueBeforeDiscount(0);
      dto.setAverageDiscountPerOrder(0.0);
      dto.setAverageOrderValue(0.0);
      dto.setUsageRate(0.0);
      dto.setConversionRate(0.0);
      dto.setDiscountPercentage(0.0);
      dto.setLastUsedAt(null);
    }
  }

  private String getCouponStatusLabel(
      Boolean status, LocalDateTime startDate, LocalDateTime endDate, LocalDateTime now) {
    if (!status) {
      return "Ẩn";
    }
    if (now.isBefore(startDate)) {
      return "Sắp có hiệu lực";
    }
    if (now.isAfter(endDate)) {
      return "Hết hạn";
    }
    return "Kích hoạt";
  }

  private LocalDateTime toLocalDateTime(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Timestamp) {
      return ((Timestamp) obj).toLocalDateTime();
    }
    if (obj instanceof LocalDateTime) {
      return (LocalDateTime) obj;
    }
    return null;
  }
}
