package vn.edu.iuh.fit.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.PriceList;

@Repository
public interface PriceListRepository extends JpaRepository<PriceList, Integer> {

  // Tìm bảng giá theo trạng thái
  List<PriceList> findByStatus(Boolean status);

  // Tìm bảng giá hiệu lực tại thời điểm hiện tại, sắp xếp theo priority giảm dần
  @Query(
      "SELECT pl FROM PriceList pl WHERE pl.status = true "
          + "AND (pl.validFrom IS NULL OR pl.validFrom <= :checkDate) "
          + "AND (pl.validTo IS NULL OR pl.validTo >= :checkDate) "
          + "ORDER BY pl.priority DESC")
  List<PriceList> findValidPriceListsAt(@Param("checkDate") Date checkDate);

  // Tìm bảng giá hiệu lực có priority cao nhất
  @Query(
      "SELECT pl FROM PriceList pl WHERE pl.status = true "
          + "AND (pl.validFrom IS NULL OR pl.validFrom <= :checkDate) "
          + "AND (pl.validTo IS NULL OR pl.validTo >= :checkDate) "
          + "ORDER BY pl.priority DESC LIMIT 1")
  PriceList findTopValidPriceListAt(@Param("checkDate") Date checkDate);

  // Tìm bảng giá theo khoảng thời gian
  @Query(
      "SELECT pl FROM PriceList pl WHERE pl.status = true "
          + "AND ((pl.validFrom IS NULL OR pl.validFrom <= :endDate) "
          + "AND (pl.validTo IS NULL OR pl.validTo >= :startDate))")
  List<PriceList> findPriceListsInDateRange(
      @Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
