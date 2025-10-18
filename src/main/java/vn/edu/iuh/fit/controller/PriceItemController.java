package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.dto.PriceItemRequest;
import vn.edu.iuh.fit.model.enums.*;
import vn.edu.iuh.fit.service.PriceListService;
import vn.edu.iuh.fit.service.PricingService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PriceItemController {

  private final PricingService pricingService;
  private final PriceListService priceListService;

  // ============= ADMIN CRUD ENDPOINTS =============

  @GetMapping("/admin/price-items")
  public ResponseEntity<?> getAllPriceItems() {
    return ResponseEntity.ok(priceListService.getAllPriceItems());
  }

  @GetMapping("/admin/price-items/{id}")
  public ResponseEntity<?> getPriceItemById(@PathVariable Integer id) {
    Optional<PriceItem> priceItem = priceListService.getPriceItemById(id);
    return priceItem.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/admin/price-items")
  public ResponseEntity<?> createPriceItem(@Valid @RequestBody PriceItemRequest priceItemRequest) {
    PriceItem createdPriceItem = priceListService.createPriceItem(priceItemRequest);
    return new ResponseEntity<>(createdPriceItem, HttpStatus.CREATED);
  }

  @PutMapping("/admin/price-items/{id}")
  public ResponseEntity<?> updatePriceItem(
      @PathVariable Integer id, @Valid @RequestBody PriceItemRequest priceItemRequest) {
    PriceItem updatedPriceItem = priceListService.updatePriceItem(id, priceItemRequest);
    return ResponseEntity.ok(updatedPriceItem);
  }

  @DeleteMapping("/admin/price-items/{id}")
  public ResponseEntity<?> deletePriceItem(@PathVariable Integer id) {
    priceListService.deletePriceItem(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/admin/price-items/{id}/toggle-status")
  public ResponseEntity<?> togglePriceItemStatus(@PathVariable Integer id) {
    PriceItem priceItem = priceListService.togglePriceItemStatus(id);
    return ResponseEntity.ok(priceItem);
  }

  @GetMapping("/admin/price-items/by-price-list/{priceListId}")
  public ResponseEntity<?> getPriceItemsByPriceListId(@PathVariable Integer priceListId) {
    return ResponseEntity.ok(priceListService.getPriceItemsByPriceListId(priceListId));
  }

  @GetMapping("/admin/price-items/by-target-type/{targetType}")
  public ResponseEntity<?> getPriceItemsByTargetType(@PathVariable TargetType targetType) {
    return ResponseEntity.ok(priceListService.getPriceItemsByTargetType(targetType));
  }

  @GetMapping("/admin/price-items/status/{status}")
  public ResponseEntity<?> getPriceItemsByStatus(@PathVariable Boolean status) {
    return ResponseEntity.ok(priceListService.getPriceItemsByStatus(status));
  }

  // ============= ADMIN PRICING ANALYSIS ENDPOINTS =============

  @GetMapping("/admin/price-items/product/{productId}/analysis")
  public ResponseEntity<?> getAllValidPriceItemsForProduct(@PathVariable Integer productId) {
    List<PriceItem> priceItems = pricingService.getAllValidPriceItemsForProduct(productId);
    return ResponseEntity.ok(priceItems);
  }

  @GetMapping("/admin/price-items/additional-service/{serviceId}/analysis")
  public ResponseEntity<?> getAllValidPriceItemsForAdditionalService(
      @PathVariable Integer serviceId) {
    List<PriceItem> priceItems =
        pricingService.getAllValidPriceItemsForAdditionalService(serviceId);
    return ResponseEntity.ok(priceItems);
  }

  @GetMapping("/admin/price-items/ticket/analysis")
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

  // ============= PUBLIC PRICING ENDPOINTS =============
  // These endpoints are used by frontend to calculate prices during booking

  @GetMapping("/public/price-items/product/{productId}/price")
  public ResponseEntity<?> getPriceForProduct(@PathVariable Integer productId) {
    Optional<Integer> price = pricingService.getPriceForProduct(productId);
    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/public/price-items/product/{productId}/quantity/{quantity}/price")
  public ResponseEntity<?> getPriceForProductWithQuantity(
      @PathVariable Integer productId, @PathVariable Integer quantity) {
    Optional<Integer> price = pricingService.getPriceForProductWithQuantity(productId, quantity);
    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/public/price-items/additional-service/{serviceId}/price")
  public ResponseEntity<?> getPriceForAdditionalService(@PathVariable Integer serviceId) {
    Optional<Integer> price = pricingService.getPriceForAdditionalService(serviceId);
    return price.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/public/price-items/ticket/price")
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

  @GetMapping("/public/price-items/ticket/price/at/{date}")
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

  // ============= PUBLIC EFFECTIVE PRICE ITEMS ENDPOINTS =============

  @GetMapping("/public/price-items/effective")
  public ResponseEntity<?> getEffectivePriceItems() {
    return ResponseEntity.ok(priceListService.getEffectivePriceItemsNow());
  }

  @GetMapping("/public/price-items/effective/{date}")
  public ResponseEntity<?> getEffectivePriceItemsAt(
      @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
    return ResponseEntity.ok(priceListService.getEffectivePriceItemsAt(date));
  }

  @GetMapping("/public/price-items/product/{productId}")
  public ResponseEntity<?> getEffectivePriceItemsForProduct(@PathVariable Integer productId) {
    List<PriceItem> priceItems = pricingService.getAllValidPriceItemsForProduct(productId);
    return ResponseEntity.ok(priceItems);
  }

  @GetMapping("/public/price-items/additional-service/{serviceId}")
  public ResponseEntity<?> getEffectivePriceItemsForAdditionalService(
      @PathVariable Integer serviceId) {
    List<PriceItem> priceItems =
        pricingService.getAllValidPriceItemsForAdditionalService(serviceId);
    return ResponseEntity.ok(priceItems);
  }

  @GetMapping("/public/price-items/ticket")
  public ResponseEntity<?> getEffectivePriceItemsForTicket(
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
