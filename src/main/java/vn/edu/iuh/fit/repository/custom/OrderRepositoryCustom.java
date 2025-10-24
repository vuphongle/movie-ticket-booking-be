package vn.edu.iuh.fit.repository.custom;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.iuh.fit.model.dto.CinemaMovieRevenueDto;
import vn.edu.iuh.fit.model.dto.CinemaRevenueDto;
import vn.edu.iuh.fit.model.dto.CustomerMovieRevenueDto;
import vn.edu.iuh.fit.model.dto.CustomerRevenueDto;
import vn.edu.iuh.fit.model.dto.MovieCinemaRevenueDto;
import vn.edu.iuh.fit.model.dto.MovieRevenueDto;
import vn.edu.iuh.fit.model.dto.RevenueDto;

public interface OrderRepositoryCustom {
  List<MovieRevenueDto> findMovieRevenues(LocalDateTime startDate, LocalDateTime endDate);

  List<CinemaRevenueDto> findCinemaRevenues(LocalDateTime startDate, LocalDateTime endDate);

  List<RevenueDto> findMonthlyRevenue();

  // Thống kê theo 1 phim cụ thể (group by cinema)
  List<MovieCinemaRevenueDto> findMovieCinemaRevenues(
      Integer movieId, LocalDateTime startDate, LocalDateTime endDate);

  // Thống kê theo 1 rạp cụ thể (group by movie)
  List<CinemaMovieRevenueDto> findCinemaMovieRevenues(
      Integer cinemaId, LocalDateTime startDate, LocalDateTime endDate);

  // Thống kê theo khách hàng
  List<CustomerRevenueDto> findCustomerRevenues(LocalDateTime startDate, LocalDateTime endDate);

  // Thống kê theo 1 khách hàng cụ thể (group by movie)
  List<CustomerMovieRevenueDto> findCustomerMovieRevenues(
      Integer customerId, LocalDateTime startDate, LocalDateTime endDate);
}
