package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertBaseTicketPriceRequest;
import vn.edu.iuh.fit.service.BaseTicketPriceService;

@Slf4j
@RestController
@RequestMapping("api/admin/base-ticket-prices")
@RequiredArgsConstructor
public class BaseTicketPriceController {
    private final BaseTicketPriceService baseTicketPriceService;

    @GetMapping
    public ResponseEntity<?> getAllBaseTicketPrices() {
        return ResponseEntity.ok(baseTicketPriceService.getAllBaseTicketPrices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBaseTicketPriceById(@PathVariable Integer id) {
        return ResponseEntity.ok(baseTicketPriceService.getBaseTicketPriceById(id));
    }

    @PostMapping
    public ResponseEntity<?> createBaseTicketPrice(@Valid @RequestBody UpsertBaseTicketPriceRequest request) {
        return new ResponseEntity<>(baseTicketPriceService.saveBaseTicketPrice(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBaseTicketPrice(@PathVariable Integer id, @Valid @RequestBody UpsertBaseTicketPriceRequest request) {
        return ResponseEntity.ok(baseTicketPriceService.updateBaseTicketPrice(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBaseTicketPrice(@PathVariable Integer id) {
        baseTicketPriceService.deleteBaseTicketPrice(id);
        return ResponseEntity.noContent().build();
    }
}
