package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.enums.*;

import java.util.Date;
import java.util.List;

@Repository
public interface PriceItemRepository extends JpaRepository<PriceItem, Integer> {
    
    // Tìm price item theo price list
    List<PriceItem> findByPriceListId(Integer priceListId);
    
    // Tìm price item theo target type và target id
    List<PriceItem> findByTargetTypeAndTargetId(TargetType targetType, Integer targetId);
    
    // Tìm price item cho sản phẩm
    @Query("SELECT pi FROM PriceItem pi JOIN pi.priceList pl " +
           "WHERE pi.targetType = 'PRODUCT' AND pi.targetId = :productId " +
           "AND pi.status = true AND pl.status = true " +
           "AND (pl.validFrom IS NULL OR pl.validFrom <= :checkDate) " +
           "AND (pl.validTo IS NULL OR pl.validTo >= :checkDate) " +
           "AND (pi.effectiveFrom IS NULL OR pi.effectiveFrom <= :checkDate) " +
           "AND (pi.effectiveTo IS NULL OR pi.effectiveTo >= :checkDate) " +
           "ORDER BY pl.priority DESC, pi.priority DESC")
    List<PriceItem> findValidPriceItemsForProduct(@Param("productId") Integer productId, 
                                                  @Param("checkDate") Date checkDate);
    
    // Tìm price item cho additional service
    @Query("SELECT pi FROM PriceItem pi JOIN pi.priceList pl " +
           "WHERE pi.targetType = 'ADDITIONAL_SERVICE' AND pi.targetId = :serviceId " +
           "AND pi.status = true AND pl.status = true " +
           "AND (pl.validFrom IS NULL OR pl.validFrom <= :checkDate) " +
           "AND (pl.validTo IS NULL OR pl.validTo >= :checkDate) " +
           "AND (pi.effectiveFrom IS NULL OR pi.effectiveFrom <= :checkDate) " +
           "AND (pi.effectiveTo IS NULL OR pi.effectiveTo >= :checkDate) " +
           "ORDER BY pl.priority DESC, pi.priority DESC")
    List<PriceItem> findValidPriceItemsForAdditionalService(@Param("serviceId") Integer serviceId, 
                                                           @Param("checkDate") Date checkDate);
    
    // Tìm price item cho vé với điều kiện cụ thể
    @Query("SELECT pi FROM PriceItem pi JOIN pi.priceList pl " +
           "WHERE pi.targetType = 'TICKET' " +
           "AND pi.status = true AND pl.status = true " +
           "AND (pl.validFrom IS NULL OR pl.validFrom <= :checkDate) " +
           "AND (pl.validTo IS NULL OR pl.validTo >= :checkDate) " +
           "AND (pi.effectiveFrom IS NULL OR pi.effectiveFrom <= :checkDate) " +
           "AND (pi.effectiveTo IS NULL OR pi.effectiveTo >= :checkDate) " +
           "AND (pi.seatType IS NULL OR pi.seatType = :seatType) " +
           "AND (pi.graphicsType IS NULL OR pi.graphicsType = :graphicsType) " +
           "AND (pi.screeningTimeType IS NULL OR pi.screeningTimeType = :screeningTimeType) " +
           "AND (pi.dayType IS NULL OR pi.dayType = :dayType) " +
           "AND (pi.auditoriumType IS NULL OR pi.auditoriumType = :auditoriumType) " +
           "ORDER BY pl.priority DESC, pi.priority DESC")
    List<PriceItem> findValidPriceItemsForTicket(@Param("seatType") SeatType seatType,
                                                @Param("graphicsType") GraphicsType graphicsType,
                                                @Param("screeningTimeType") ScreeningTimeType screeningTimeType,
                                                @Param("dayType") DayType dayType,
                                                @Param("auditoriumType") AuditoriumType auditoriumType,
                                                @Param("checkDate") Date checkDate);
    
    // Tìm price item theo price list và target type
    List<PriceItem> findByPriceListIdAndTargetType(Integer priceListId, TargetType targetType);
}