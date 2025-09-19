package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.enums.*;
import vn.edu.iuh.fit.service.PricingService;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PricingController {
    
    private final PricingService pricingService;
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<Integer> getPriceForProduct(@PathVariable Integer productId) {
        Optional<Integer> price = pricingService.getPriceForProduct(productId);
        return price.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/product/{productId}/quantity/{quantity}")
    public ResponseEntity<Integer> getPriceForProductWithQuantity(@PathVariable Integer productId,
                                                                @PathVariable Integer quantity) {
        Optional<Integer> price = pricingService.getPriceForProductWithQuantity(productId, quantity);
        return price.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/additional-service/{serviceId}")
    public ResponseEntity<Integer> getPriceForAdditionalService(@PathVariable Integer serviceId) {
        Optional<Integer> price = pricingService.getPriceForAdditionalService(serviceId);
        return price.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/ticket")
    public ResponseEntity<Integer> getPriceForTicket(
            @RequestParam SeatType seatType,
            @RequestParam GraphicsType graphicsType,
            @RequestParam ScreeningTimeType screeningTimeType,
            @RequestParam DayType dayType,
            @RequestParam AuditoriumType auditoriumType) {
        
        Optional<Integer> price = pricingService.getPriceForTicket(
                seatType, graphicsType, screeningTimeType, dayType, auditoriumType);
        
        return price.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/ticket/at/{date}")
    public ResponseEntity<Integer> getPriceForTicketAt(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestParam SeatType seatType,
            @RequestParam GraphicsType graphicsType,
            @RequestParam ScreeningTimeType screeningTimeType,
            @RequestParam DayType dayType,
            @RequestParam AuditoriumType auditoriumType) {
        
        Optional<Integer> price = pricingService.getPriceForTicket(
                seatType, graphicsType, screeningTimeType, dayType, auditoriumType, date);
        
        return price.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/product/{productId}/price-items")
    public ResponseEntity<List<PriceItem>> getAllValidPriceItemsForProduct(@PathVariable Integer productId) {
        List<PriceItem> priceItems = pricingService.getAllValidPriceItemsForProduct(productId);
        return ResponseEntity.ok(priceItems);
    }
    
    @GetMapping("/additional-service/{serviceId}/price-items")
    public ResponseEntity<List<PriceItem>> getAllValidPriceItemsForAdditionalService(@PathVariable Integer serviceId) {
        List<PriceItem> priceItems = pricingService.getAllValidPriceItemsForAdditionalService(serviceId);
        return ResponseEntity.ok(priceItems);
    }
    
    @GetMapping("/ticket/price-items")
    public ResponseEntity<List<PriceItem>> getAllValidPriceItemsForTicket(
            @RequestParam SeatType seatType,
            @RequestParam GraphicsType graphicsType,
            @RequestParam ScreeningTimeType screeningTimeType,
            @RequestParam DayType dayType,
            @RequestParam AuditoriumType auditoriumType) {
        
        List<PriceItem> priceItems = pricingService.getAllValidPriceItemsForTicket(
                seatType, graphicsType, screeningTimeType, dayType, auditoriumType);
        
        return ResponseEntity.ok(priceItems);
    }
}