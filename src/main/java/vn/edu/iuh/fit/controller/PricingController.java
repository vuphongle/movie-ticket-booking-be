package vn.edu.iuh.fit.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.enums.*;
import vn.edu.iuh.fit.service.PricingService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PricingController {

  private final PricingService pricingService;

  @GetMapping("/public/pricing/product/{productId}")
  public ResponseEntity<?> getPriceForProduct(@PathVariable Integer productId) {
    Optional<Integer> price = pricingService.getPriceForProduct(productId);
    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/public/pricing/product/{productId}/quantity/{quantity}")
  public ResponseEntity<?> getPriceForProductWithQuantity(
      @PathVariable Integer productId, @PathVariable Integer quantity) {
    Optional<Integer> price = pricingService.getPriceForProductWithQuantity(productId, quantity);
    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/public/pricing/additional-service/{serviceId}")
  public ResponseEntity<?> getPriceForAdditionalService(@PathVariable Integer serviceId) {
    Optional<Integer> price = pricingService.getPriceForAdditionalService(serviceId);
    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/public/pricing/ticket")
  public ResponseEntity<?> getPriceForTicket(
      @RequestParam SeatType seatType,
      @RequestParam GraphicsType graphicsType,
      @RequestParam ScreeningTimeType screeningTimeType,
      @RequestParam DayType dayType,
      @RequestParam AuditoriumType auditoriumType) {

    Optional<Integer> price =
        pricingService.getPriceForTicket(
            seatType, graphicsType, screeningTimeType, dayType, auditoriumType);

    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/public/pricing/ticket/at/{date}")
  public ResponseEntity<?> getPriceForTicketAt(
      @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
      @RequestParam SeatType seatType,
      @RequestParam GraphicsType graphicsType,
      @RequestParam ScreeningTimeType screeningTimeType,
      @RequestParam DayType dayType,
      @RequestParam AuditoriumType auditoriumType) {

    Optional<Integer> price =
        pricingService.getPriceForTicket(
            seatType, graphicsType, screeningTimeType, dayType, auditoriumType, date);

    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/admin/pricing/product/{productId}/analysis")
  public ResponseEntity<?> getAllValidPriceItemsForProduct(@PathVariable Integer productId) {
    List<PriceItem> priceItems = pricingService.getAllValidPriceItemsForProduct(productId);
    return ResponseEntity.ok(priceItems);
  }

  @GetMapping("/admin/pricing/additional-service/{serviceId}/analysis")
  public ResponseEntity<?> getAllValidPriceItemsForAdditionalService(
      @PathVariable Integer serviceId) {
    List<PriceItem> priceItems =
        pricingService.getAllValidPriceItemsForAdditionalService(serviceId);
    return ResponseEntity.ok(priceItems);
  }

  @GetMapping("/admin/pricing/ticket/analysis")
  public ResponseEntity<?> getAllValidPriceItemsForTicket(
      @RequestParam SeatType seatType,
      @RequestParam GraphicsType graphicsType,
      @RequestParam ScreeningTimeType screeningTimeType,
      @RequestParam DayType dayType,
      @RequestParam AuditoriumType auditoriumType) {

    List<PriceItem> priceItems =
        pricingService.getAllValidPriceItemsForTicket(
            seatType, graphicsType, screeningTimeType, dayType, auditoriumType);

    return ResponseEntity.ok(priceItems);
  }
}
