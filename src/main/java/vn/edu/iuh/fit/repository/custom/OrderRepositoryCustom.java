package vn.edu.iuh.fit.repository.custom;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.iuh.fit.model.dto.CinemaRevenueDto;
import vn.edu.iuh.fit.model.dto.MovieRevenueDto;
import vn.edu.iuh.fit.model.dto.RevenueDto;

public interface OrderRepositoryCustom {
  List<MovieRevenueDto> findMovieRevenues(LocalDateTime startDate, LocalDateTime endDate);

  List<CinemaRevenueDto> findCinemaRevenues(LocalDateTime startDate, LocalDateTime endDate);

  List<RevenueDto> findMonthlyRevenue();
}
