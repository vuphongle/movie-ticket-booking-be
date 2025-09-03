package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.service.AdditionalServices;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class AdditionalServiceController {
    private final AdditionalServices additionalServices;

    @GetMapping("/public/additional-services")
    public ResponseEntity<?> getAllAdditionalServicesByStatus() {
        return ResponseEntity.ok(additionalServices.getAllAdditionalServicesByStatus(true));
    }
}
