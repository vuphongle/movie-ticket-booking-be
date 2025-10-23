package vn.edu.iuh.fit.repository.custom.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.Order;
import vn.edu.iuh.fit.model.dto.CinemaRevenueDto;
import vn.edu.iuh.fit.model.dto.MovieRevenueDto;
import vn.edu.iuh.fit.model.dto.RevenueDto;
import vn.edu.iuh.fit.repository.custom.OrderRepositoryCustom;

@Slf4j
@Repository
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public List<MovieRevenueDto> findMovieRevenues(LocalDateTime startDate, LocalDateTime endDate) {
    List<Order> orders =
        entityManager
            .createQuery(
                "SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = 'CONFIRMED'",
                Order.class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

    HashMap<Integer, MovieRevenueDto> movieRevenueMap = new HashMap<>();
    for (Order order : orders) {
      Integer movieId = order.getShowtime().getMovie().getId();
      String movieCode = "P" + String.format("%04d", movieId);
      String movieName = order.getShowtime().getMovie().getName();
      Integer ticketCount = order.getTicketItems().size();
      Integer discount = order.getDiscount() != null ? order.getDiscount() : 0;
      Integer tempPrice = order.getTempPrice();
      Integer totalRevenue = order.getTotalPrice();

      MovieRevenueDto movieRevenueDto = movieRevenueMap.get(movieId);
      if (movieRevenueDto == null) {
        movieRevenueDto =
            new MovieRevenueDto(
                movieId, movieCode, movieName, ticketCount, discount, tempPrice, totalRevenue);
        movieRevenueMap.put(movieId, movieRevenueDto);
      } else {
        movieRevenueDto.setTotalTickets(movieRevenueDto.getTotalTickets() + ticketCount);
        movieRevenueDto.setTotalDiscount(movieRevenueDto.getTotalDiscount() + discount);
        movieRevenueDto.setRevenueBeforeDiscount(
            movieRevenueDto.getRevenueBeforeDiscount() + tempPrice);
        movieRevenueDto.setTotalRevenue(movieRevenueDto.getTotalRevenue() + totalRevenue);
      }
    }

    return new ArrayList<>(movieRevenueMap.values());
  }

  @Override
  public List<CinemaRevenueDto> findCinemaRevenues(LocalDateTime startDate, LocalDateTime endDate) {
    List<Order> orders =
        entityManager
            .createQuery(
                "SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = 'CONFIRMED'",
                Order.class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

    HashMap<Integer, CinemaRevenueDto> cinemaRevenueMap = new HashMap<>();
    for (Order order : orders) {
      Integer cinemaId = order.getShowtime().getAuditorium().getCinema().getId();
      String cinemaCode = "R" + String.format("%04d", cinemaId);
      String cinemaName = order.getShowtime().getAuditorium().getCinema().getName();
      Integer ticketCount = order.getTicketItems().size();
      Integer discount = order.getDiscount() != null ? order.getDiscount() : 0;

      // Calculate ticket revenue
      Integer ticketRevenue =
          order.getTicketItems().stream().map(item -> item.getPrice()).reduce(0, Integer::sum);

      // Calculate service revenue
      Integer serviceRevenue =
          order.getServiceItems().stream()
              .map(item -> item.getPrice() * item.getQuantity())
              .reduce(0, Integer::sum);

      Integer tempPrice = ticketRevenue + serviceRevenue;
      Integer totalRevenue = order.getTotalPrice();

      CinemaRevenueDto cinemaRevenueDto = cinemaRevenueMap.get(cinemaId);
      if (cinemaRevenueDto == null) {
        cinemaRevenueDto =
            new CinemaRevenueDto(
                cinemaId,
                cinemaCode,
                cinemaName,
                ticketCount,
                ticketRevenue,
                serviceRevenue,
                discount,
                tempPrice,
                totalRevenue);
        cinemaRevenueMap.put(cinemaId, cinemaRevenueDto);
      } else {
        cinemaRevenueDto.setTotalTickets(cinemaRevenueDto.getTotalTickets() + ticketCount);
        cinemaRevenueDto.setTicketRevenue(cinemaRevenueDto.getTicketRevenue() + ticketRevenue);
        cinemaRevenueDto.setServiceRevenue(cinemaRevenueDto.getServiceRevenue() + serviceRevenue);
        cinemaRevenueDto.setTotalDiscount(cinemaRevenueDto.getTotalDiscount() + discount);
        cinemaRevenueDto.setRevenueBeforeDiscount(
            cinemaRevenueDto.getRevenueBeforeDiscount() + tempPrice);
        cinemaRevenueDto.setTotalRevenue(cinemaRevenueDto.getTotalRevenue() + totalRevenue);
      }
    }

    return new ArrayList<>(cinemaRevenueMap.values());
  }

  @Override
  public List<RevenueDto> findMonthlyRevenue() {
    LocalDate today = LocalDate.now();
    List<LocalDate> lastFiveMonthsStarts =
        IntStream.rangeClosed(0, 4).mapToObj(i -> today.minusMonths(i).withDayOfMonth(1)).toList();

    return lastFiveMonthsStarts.stream()
        .map(this::calculateRevenueForMonth)
        .sorted(Comparator.comparing(RevenueDto::getYear).thenComparing(RevenueDto::getMonth))
        .collect(Collectors.toList());
  }

  private RevenueDto calculateRevenueForMonth(LocalDate monthStart) {
    LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
    log.info(
        "Calculating revenue for month: {}/{} (from {} to {})",
        monthStart.getMonthValue(),
        monthStart.getYear(),
        monthStart,
        monthEnd);

    String sql =
        "SELECT EXTRACT(MONTH FROM o.created_at) AS month, "
            + "EXTRACT(YEAR FROM o.created_at) AS year, "
            + "SUM("
            + "  GREATEST("
            + "    (COALESCE((SELECT SUM(oti.price) FROM order_ticket_items oti WHERE oti.order_id = o.id), 0) + "
            + "     COALESCE((SELECT SUM(osi.price * osi.quantity) FROM order_service_items osi WHERE osi.order_id = o.id), 0) - "
            + "     COALESCE(o.discount, 0)), "
            + "    0"
            + "  )"
            + ") AS revenue "
            + "FROM orders o "
            + "WHERE o.created_at BETWEEN ?1 AND ?2 "
            + "AND o.status = 'CONFIRMED' "
            + "GROUP BY EXTRACT(YEAR FROM o.created_at), EXTRACT(MONTH FROM o.created_at)";

    List<Object[]> results =
        entityManager
            .createNativeQuery(sql)
            .setParameter(1, java.sql.Date.valueOf(monthStart))
            .setParameter(2, java.sql.Date.valueOf(monthEnd))
            .getResultList();

    if (results.isEmpty()) {
      log.info(
          "No revenue found for month {}/{}, returning 0",
          monthStart.getMonthValue(),
          monthStart.getYear());
      return new RevenueDto(monthStart.getMonthValue(), monthStart.getYear(), 0);
    } else {
      Object[] result = results.get(0);
      int month = ((Number) result[0]).intValue();
      int year = ((Number) result[1]).intValue();
      long revenue = ((Number) result[2]).longValue();
      log.info("Revenue for month {}/{}: {}", month, year, revenue);
      return new RevenueDto(month, year, revenue);
    }
  }
}
