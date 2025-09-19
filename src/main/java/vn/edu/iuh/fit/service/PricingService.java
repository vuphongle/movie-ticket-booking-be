package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.enums.*;
import vn.edu.iuh.fit.repository.PriceItemRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PricingService {
    
    private final PriceItemRepository priceItemRepository;
    
    // Tính giá cho sản phẩm
    public Optional<Integer> getPriceForProduct(Integer productId) {
        return getPriceForProduct(productId, new Date());
    }
    
    public Optional<Integer> getPriceForProduct(Integer productId, Date checkDate) {
        List<PriceItem> priceItems = priceItemRepository.findValidPriceItemsForProduct(productId, checkDate);
        
        if (priceItems.isEmpty()) {
            return Optional.empty();
        }
        
        // Lấy price item đầu tiên (đã được sắp xếp theo priority)
        return Optional.of(priceItems.get(0).getPrice());
    }
    
    // Tính giá cho additional service
    public Optional<Integer> getPriceForAdditionalService(Integer serviceId) {
        return getPriceForAdditionalService(serviceId, new Date());
    }
    
    public Optional<Integer> getPriceForAdditionalService(Integer serviceId, Date checkDate) {
        List<PriceItem> priceItems = priceItemRepository.findValidPriceItemsForAdditionalService(serviceId, checkDate);
        
        if (priceItems.isEmpty()) {
            return Optional.empty();
        }
        
        // Lấy price item đầu tiên (đã được sắp xếp theo priority)
        return Optional.of(priceItems.get(0).getPrice());
    }
    
    // Tính giá cho vé xem phim
    public Optional<Integer> getPriceForTicket(SeatType seatType, GraphicsType graphicsType,
                                             ScreeningTimeType screeningTimeType, DayType dayType,
                                             AuditoriumType auditoriumType) {
        return getPriceForTicket(seatType, graphicsType, screeningTimeType, dayType, auditoriumType, new Date());
    }
    
    public Optional<Integer> getPriceForTicket(SeatType seatType, GraphicsType graphicsType,
                                             ScreeningTimeType screeningTimeType, DayType dayType,
                                             AuditoriumType auditoriumType, Date checkDate) {
        List<PriceItem> priceItems = priceItemRepository.findValidPriceItemsForTicket(
                seatType, graphicsType, screeningTimeType, dayType, auditoriumType, checkDate);
        
        if (priceItems.isEmpty()) {
            return Optional.empty();
        }
        
        // Lấy price item đầu tiên (đã được sắp xếp theo priority)
        return Optional.of(priceItems.get(0).getPrice());
    }
    
    // Tính giá với số lượng tối thiểu
    public Optional<Integer> getPriceForProductWithQuantity(Integer productId, Integer quantity) {
        return getPriceForProductWithQuantity(productId, quantity, new Date());
    }
    
    public Optional<Integer> getPriceForProductWithQuantity(Integer productId, Integer quantity, Date checkDate) {
        List<PriceItem> priceItems = priceItemRepository.findValidPriceItemsForProduct(productId, checkDate);
        
        // Lọc theo số lượng tối thiểu
        Optional<PriceItem> matchedPriceItem = priceItems.stream()
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
    
    public List<PriceItem> getAllValidPriceItemsForTicket(SeatType seatType, GraphicsType graphicsType,
                                                         ScreeningTimeType screeningTimeType, DayType dayType,
                                                         AuditoriumType auditoriumType) {
        return priceItemRepository.findValidPriceItemsForTicket(
                seatType, graphicsType, screeningTimeType, dayType, auditoriumType, new Date());
    }
}