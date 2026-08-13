package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.AdditionalServiceItem;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.request.UpsertAdditionalServiceRequest;
import vn.edu.iuh.fit.service.AdditionalServices;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdditionalServiceController {
  private final AdditionalServices additionalServices;

  @GetMapping("/public/additional-services")
  public ResponseEntity<?> getAllAdditionalServicesByStatus() {
    return ResponseEntity.ok(additionalServices.getAllAdditionalServicesByStatus(true));
  }

  @GetMapping("/admin/additional-services")
  public ResponseEntity<?> getAllAdditionalServices() {
    return ResponseEntity.ok(additionalServices.getAllAdditionalServices());
  }

  @GetMapping("/admin/additional-services/{id}")
  public ResponseEntity<?> getAdditionalServiceById(@PathVariable Integer id) {
    return ResponseEntity.ok(additionalServices.getAdditionalServiceById(id));
  }

  @PostMapping("/admin/additional-services")
  public ResponseEntity<?> createAdditionalService(
      @Valid @RequestBody UpsertAdditionalServiceRequest request) {
    return new ResponseEntity<>(
        additionalServices.saveAdditionalService(request), HttpStatus.CREATED);
  }

  @PutMapping("/admin/additional-services/{id}")
  public ResponseEntity<?> updateAdditionalService(
      @PathVariable Integer id, @Valid @RequestBody UpsertAdditionalServiceRequest request) {
    return ResponseEntity.ok(additionalServices.updateAdditionalService(id, request));
  }

  @DeleteMapping("/admin/additional-services/{id}")
  public ResponseEntity<?> deleteAdditionalService(@PathVariable Integer id) {
    additionalServices.deleteAdditionalService(id);
    return ResponseEntity.noContent().build();
  }

  // API mới để lấy giá của additional service
  @GetMapping("/public/additional-services/{id}/price")
  public ResponseEntity<?> getAdditionalServicePrice(@PathVariable Integer id) {
    PriceItem priceItem = additionalServices.getPriceForAdditionalService(id);

    if (priceItem == null) {
      return ResponseEntity.notFound().build();
    }

    // Trả về JSON chứa price và priceId
    Map<String, Object> response =
        Map.of(
            "price", priceItem.getPrice(),
            "priceId", priceItem.getId());

    return ResponseEntity.ok(response);
  }

  // API mới để lấy các items của combo service
  @GetMapping("/public/additional-services/{id}/items")
  public ResponseEntity<List<AdditionalServiceItem>> getAdditionalServiceItems(
      @PathVariable Integer id) {
    List<AdditionalServiceItem> items = additionalServices.getAdditionalServiceItems(id);
    return ResponseEntity.ok(items);
  }
}
