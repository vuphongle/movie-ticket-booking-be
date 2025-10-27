package vn.edu.iuh.fit.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.enums.*;
import vn.edu.iuh.fit.repository.PriceItemRepository;

@Service
@RequiredArgsConstructor
public class PricingService {

  private final PriceItemRepository priceItemRepository;

  // Tính giá cho sản phẩm
  public Optional<Integer> getPriceForProduct(Integer productId) {
    return getPriceForProduct(productId, new Date());
  }

  public Optional<Integer> getPriceForProduct(Integer productId, Date checkDate) {
    return priceItemRepository.findValidPriceItemsForProduct(productId, checkDate).stream()
        .findFirst()
        .map(PriceItem::getPrice);
  }

  // Tính giá cho additional service
  public Optional<PriceItem> getPriceForAdditionalService(Integer serviceId) {
    return getPriceForAdditionalService(serviceId, new Date());
  }

  // Lấy PriceItem cho additional service theo ngày
  public Optional<PriceItem> getPriceForAdditionalService(Integer serviceId, Date checkDate) {
    if (checkDate == null) {
      throw new IllegalArgumentException("checkDate cannot be null");
    }

    return priceItemRepository
        .findValidPriceItemsForAdditionalService(serviceId, checkDate)
        .stream()
        .findFirst();
  }

  // Tính giá cho vé xem phim
  public Optional<PriceItem> getPriceItemForTicket(
      SeatType seatType,
      GraphicsType graphicsType,
      ScreeningTimeType screeningTimeType,
      DayType dayType,
      AuditoriumType auditoriumType) {
    return getPriceItemForTicket(
        seatType, graphicsType, screeningTimeType, dayType, auditoriumType, new Date());
  }

  public Optional<PriceItem> getPriceItemForTicket(
      SeatType seatType,
      GraphicsType graphicsType,
      ScreeningTimeType screeningTimeType,
      DayType dayType,
      AuditoriumType auditoriumType,
      Date checkDate) {

    if (checkDate == null) {
      throw new IllegalArgumentException("checkDate cannot be null");
    }

    List<PriceItem> priceItems =
        priceItemRepository.findValidPriceItemsForTicket(
            seatType, graphicsType, screeningTimeType, dayType, auditoriumType, checkDate);

    if (priceItems == null || priceItems.isEmpty()) {
      return Optional.empty();
    }

    // Chọn PriceItem cụ thể nhất (ít null nhất)
    PriceItem bestMatch =
        priceItems.stream()
            .filter(Objects::nonNull)
            .max(Comparator.comparingInt(this::countNonNullAttributes))
            .orElse(null);

    return Optional.ofNullable(bestMatch);
  }

  /** Đếm số lượng thuộc tính không null trong PriceItem (dùng để xác định độ cụ thể). */
  private int countNonNullAttributes(PriceItem pi) {
    int count = 0;
    if (pi.getSeatType() != null) count++;
    if (pi.getGraphicsType() != null) count++;
    if (pi.getScreeningTimeType() != null) count++;
    if (pi.getDayType() != null) count++;
    if (pi.getAuditoriumType() != null) count++;
    return count;
  }

  // Tính giá với số lượng tối thiểu
  public Optional<Integer> getPriceForProductWithQuantity(Integer productId, Integer quantity) {
    return getPriceForProductWithQuantity(productId, quantity, new Date());
  }

  public Optional<Integer> getPriceForProductWithQuantity(
      Integer productId, Integer quantity, Date checkDate) {
    List<PriceItem> priceItems =
        priceItemRepository.findValidPriceItemsForProduct(productId, checkDate);

    // Lọc theo số lượng tối thiểu
    Optional<PriceItem> matchedPriceItem =
        priceItems.stream()
            .filter(pi -> pi.getMinQty() == null || quantity >= pi.getMinQty())
            .findFirst(); // Đã được sắp xếp theo priority

    return matchedPriceItem.map(PriceItem::getPrice);
  }

  // Lấy tất cả price items cho target
  public List<PriceItem> getAllValidPriceItemsForProduct(Integer productId) {
    return priceItemRepository.findValidPriceItemsForProduct(productId, new Date());
  }

  public List<PriceItem> getAllValidPriceItemsForAdditionalService(Integer serviceId) {
    return priceItemRepository.findValidPriceItemsForAdditionalService(serviceId, new Date());
  }

  public List<PriceItem> getAllValidPriceItemsForTicket(
      SeatType seatType,
      GraphicsType graphicsType,
      ScreeningTimeType screeningTimeType,
      DayType dayType,
      AuditoriumType auditoriumType) {
    return priceItemRepository.findValidPriceItemsForTicket(
        seatType, graphicsType, screeningTimeType, dayType, auditoriumType, new Date());
  }
}
