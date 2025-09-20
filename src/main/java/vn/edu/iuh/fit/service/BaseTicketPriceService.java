package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.BaseTicketPrice;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertBaseTicketPriceRequest;
import vn.edu.iuh.fit.repository.BaseTicketPriceRepository;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated This service is deprecated. Use PricingService and PriceListService instead.
 * Maintained for backward compatibility only.
 */
@Deprecated
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseTicketPriceService {
    private final BaseTicketPriceRepository baseTicketPriceRepository;
    private final PricingService pricingService; // For migration support

    @Deprecated
    public List<BaseTicketPrice> getAllBaseTicketPrices() {
        log.warn("BaseTicketPriceService.getAllBaseTicketPrices() is deprecated. Use PriceListService instead.");
        return baseTicketPriceRepository.findAll();
    }

    @Deprecated
    public BaseTicketPrice getBaseTicketPriceById(Integer id) {
        log.warn("BaseTicketPriceService.getBaseTicketPriceById() is deprecated. Use PricingService instead.");
        return baseTicketPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá vé với id: " + id));
    }

    @Deprecated
    public BaseTicketPrice saveBaseTicketPrice(UpsertBaseTicketPriceRequest request) {
        log.warn("BaseTicketPriceService.saveBaseTicketPrice() is deprecated. Use PriceListService and PriceItem instead.");
        log.info("Creating base ticket price: {}", request);
        
        // Check if the base ticket price already exists
        Optional<BaseTicketPrice> existingBaseTicketPrice = baseTicketPriceRepository.findBySeatTypeAndGraphicsTypeAndAuditoriumTypeAndScreeningTimeTypeAndDayType(
                request.getSeatType(),
                request.getGraphicsType(),
                request.getAuditoriumType(),
                request.getScreeningTimeType(),
                request.getDayType()
        );
        if (existingBaseTicketPrice.isPresent()) {
            throw new BadRequestException("Giá vé đã tồn tại");
        }

        BaseTicketPrice baseTicketPrice = BaseTicketPrice.builder()
                .seatType(request.getSeatType())
                .graphicsType(request.getGraphicsType())
                .auditoriumType(request.getAuditoriumType())
                .screeningTimeType(request.getScreeningTimeType())
                .price(request.getPrice())
                .dayType(request.getDayType())
                .build();
        return baseTicketPriceRepository.save(baseTicketPrice);
    }

    @Deprecated
    public BaseTicketPrice updateBaseTicketPrice(Integer id, UpsertBaseTicketPriceRequest request) {
        log.warn("BaseTicketPriceService.updateBaseTicketPrice() is deprecated. Use PriceListService and PriceItem instead.");
        BaseTicketPrice baseTicketPrice = baseTicketPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá vé với id: " + id));

        Optional<BaseTicketPrice> existingBaseTicketPrice = baseTicketPriceRepository.findBySeatTypeAndGraphicsTypeAndAuditoriumTypeAndScreeningTimeTypeAndDayType(
                request.getSeatType(),
                request.getGraphicsType(),
                request.getAuditoriumType(),
                request.getScreeningTimeType(),
                request.getDayType()
        );
        if (existingBaseTicketPrice.isPresent() && !existingBaseTicketPrice.get().getId().equals(id)) {
            throw new BadRequestException("Giá vé đã tồn tại");
        }

        baseTicketPrice.setSeatType(request.getSeatType());
        baseTicketPrice.setGraphicsType(request.getGraphicsType());
        baseTicketPrice.setAuditoriumType(request.getAuditoriumType());
        baseTicketPrice.setScreeningTimeType(request.getScreeningTimeType());
        baseTicketPrice.setPrice(request.getPrice());
        baseTicketPrice.setDayType(request.getDayType());

        return baseTicketPriceRepository.save(baseTicketPrice);
    }

    @Deprecated
    public void deleteBaseTicketPrice(Integer id) {
        log.warn("BaseTicketPriceService.deleteBaseTicketPrice() is deprecated. Use PriceListService and PriceItem instead.");
        BaseTicketPrice baseTicketPrice = baseTicketPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá vé với id: " + id));
        baseTicketPriceRepository.delete(baseTicketPrice);
    }
    
    /**
     * Helper method để migrate giá từ BaseTicketPrice sang PriceItem format
     * Deprecated - chỉ dùng để migration
     */
    @Deprecated
    public Integer getTicketPriceFromNewSystem(vn.edu.iuh.fit.model.enums.SeatType seatType,
                                             vn.edu.iuh.fit.model.enums.GraphicsType graphicsType,
                                             vn.edu.iuh.fit.model.enums.ScreeningTimeType screeningTimeType,
                                             vn.edu.iuh.fit.model.enums.DayType dayType,
                                             vn.edu.iuh.fit.model.enums.AuditoriumType auditoriumType) {
        log.info("Getting ticket price from new PricingService system");
        return pricingService.getPriceForTicket(seatType, graphicsType, screeningTimeType, dayType, auditoriumType)
                .orElse(70000); // Default price
    }
}
