package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertCountryRequest;
import vn.edu.iuh.fit.service.CountryService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class CountryController {
    private final CountryService countryService;

    @GetMapping("/admin/countries")
    public ResponseEntity<?> getAllCountriesByAdmin() {
        return ResponseEntity.ok(countryService.getAllCountries());
    }

    @PostMapping("/admin/countries")
    public ResponseEntity<?> createCountry(@Valid @RequestBody UpsertCountryRequest request) {
        return new ResponseEntity<>(countryService.saveCountry(request), HttpStatus.CREATED);
    }

    @PutMapping("/admin/countries/{id}")
    public ResponseEntity<?> updateCountry(@PathVariable Integer id, @Valid @RequestBody UpsertCountryRequest request) {
        return ResponseEntity.ok(countryService.updateCountry(id, request));
    }

    @DeleteMapping("/admin/countries/{id}")
    public ResponseEntity<?> deleteCountry(@PathVariable Integer id) {
        countryService.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }
}
