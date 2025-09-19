package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.PriceList;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.service.PriceListService;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/price-lists")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PriceListController {
    
    private final PriceListService priceListService;
    
    @GetMapping
    public ResponseEntity<List<PriceList>> getAllPriceLists() {
        return ResponseEntity.ok(priceListService.getAllPriceLists());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PriceList>> getPriceListsByStatus(@PathVariable Boolean status) {
        return ResponseEntity.ok(priceListService.getPriceListsByStatus(status));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PriceList> getPriceListById(@PathVariable Integer id) {
        Optional<PriceList> priceList = priceListService.getPriceListById(id);
        return priceList.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/valid")
    public ResponseEntity<List<PriceList>> getValidPriceLists() {
        return ResponseEntity.ok(priceListService.getValidPriceListsNow());
    }
    
    @GetMapping("/valid/{date}")
    public ResponseEntity<List<PriceList>> getValidPriceListsAt(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        return ResponseEntity.ok(priceListService.getValidPriceListsAt(date));
    }
    
    @GetMapping("/top-valid")
    public ResponseEntity<PriceList> getTopValidPriceList() {
        PriceList priceList = priceListService.getTopValidPriceListNow();
        return priceList != null ? ResponseEntity.ok(priceList) 
                                 : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<PriceList> createPriceList(@Valid @RequestBody PriceList priceList) {
        PriceList createdPriceList = priceListService.createPriceList(priceList);
        return new ResponseEntity<>(createdPriceList, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PriceList> updatePriceList(@PathVariable Integer id, 
                                                   @Valid @RequestBody PriceList priceList) {
        PriceList updatedPriceList = priceListService.updatePriceList(id, priceList);
        return ResponseEntity.ok(updatedPriceList);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePriceList(@PathVariable Integer id) {
        priceListService.deletePriceList(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<PriceList> togglePriceListStatus(@PathVariable Integer id) {
        PriceList priceList = priceListService.togglePriceListStatus(id);
        return ResponseEntity.ok(priceList);
    }
    
    @GetMapping("/{id}/price-items")
    public ResponseEntity<List<PriceItem>> getPriceItemsByPriceListId(@PathVariable Integer id) {
        return ResponseEntity.ok(priceListService.getPriceItemsByPriceListId(id));
    }
}