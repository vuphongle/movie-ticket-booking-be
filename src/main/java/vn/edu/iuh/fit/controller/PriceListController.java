package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.PriceList;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.dto.ClonePriceListRequest;
import vn.edu.iuh.fit.service.PriceListService;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PriceListController {
    
    private final PriceListService priceListService;

    @GetMapping("/admin/price-lists")
    public ResponseEntity<?> getAllPriceLists() {
        return ResponseEntity.ok(priceListService.getAllPriceLists());
    }
    
    @GetMapping("/admin/price-lists/{id}")
    public ResponseEntity<?> getPriceListById(@PathVariable Integer id) {
        Optional<PriceList> priceList = priceListService.getPriceListById(id);
        return priceList.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/admin/price-lists")
    public ResponseEntity<?> createPriceList(@Valid @RequestBody PriceList priceList) {
        PriceList createdPriceList = priceListService.createPriceList(priceList);
        return new ResponseEntity<>(createdPriceList, HttpStatus.CREATED);
    }
    
    @PutMapping("/admin/price-lists/{id}")
    public ResponseEntity<?> updatePriceList(@PathVariable Integer id, 
                                           @Valid @RequestBody PriceList priceList) {
        PriceList updatedPriceList = priceListService.updatePriceList(id, priceList);
        return ResponseEntity.ok(updatedPriceList);
    }
    
    @DeleteMapping("/admin/price-lists/{id}")
    public ResponseEntity<?> deletePriceList(@PathVariable Integer id) {
        priceListService.deletePriceList(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/admin/price-lists/{id}/toggle-status")
    public ResponseEntity<?> togglePriceListStatus(@PathVariable Integer id) {
        PriceList priceList = priceListService.togglePriceListStatus(id);
        return ResponseEntity.ok(priceList);
    }
    
    @PostMapping("/admin/price-lists/{id}/clone")
    public ResponseEntity<?> clonePriceList(@PathVariable Integer id, 
                                          @Valid @RequestBody ClonePriceListRequest request) {
        try {
            PriceList clonedPriceList = priceListService.clonePriceList(id, request);
            return new ResponseEntity<>(clonedPriceList, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Error cloning price list with id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Error cloning price list: " + e.getMessage());
        }
    }
    
    @GetMapping("/admin/price-lists/status/{status}")
    public ResponseEntity<?> getPriceListsByStatus(@PathVariable Boolean status) {
        return ResponseEntity.ok(priceListService.getPriceListsByStatus(status));
    }
    
    @GetMapping("/admin/price-lists/{id}/price-items")
    public ResponseEntity<?> getPriceItemsByPriceListId(@PathVariable Integer id) {
        return ResponseEntity.ok(priceListService.getPriceItemsByPriceListId(id));
    }
    
    // ============= PUBLIC ENDPOINTS =============
    
    @GetMapping("/public/price-lists/valid")
    public ResponseEntity<?> getValidPriceLists() {
        return ResponseEntity.ok(priceListService.getValidPriceListsNow());
    }
    
    @GetMapping("/public/price-lists/valid/{date}")
    public ResponseEntity<?> getValidPriceListsAt(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        return ResponseEntity.ok(priceListService.getValidPriceListsAt(date));
    }
    
    @GetMapping("/public/price-lists/top-valid")
    public ResponseEntity<?> getTopValidPriceList() {
        PriceList priceList = priceListService.getTopValidPriceListNow();
        return priceList != null ? ResponseEntity.ok(priceList) 
                                 : ResponseEntity.notFound().build();
    }
}