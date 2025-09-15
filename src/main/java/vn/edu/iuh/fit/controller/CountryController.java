package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
}
