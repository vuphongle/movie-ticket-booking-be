package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertAdditionalServiceRequest;
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

    @GetMapping("/admin/additional-services")
    public ResponseEntity<?> getAllAdditionalServices() {
        return ResponseEntity.ok(additionalServices.getAllAdditionalServices());
    }

    @GetMapping("/admin/additional-services/{id}")
    public ResponseEntity<?> getAdditionalServiceById(@PathVariable Integer id) {
        return ResponseEntity.ok(additionalServices.getAdditionalServiceById(id));
    }

    @PostMapping("/admin/additional-services")
    public ResponseEntity<?> createAdditionalService(@Valid @RequestBody UpsertAdditionalServiceRequest request) {
        return new ResponseEntity<>(additionalServices.saveAdditionalService(request), HttpStatus.CREATED);
    }

    @PutMapping("/admin/additional-services/{id}")
    public ResponseEntity<?> updateAdditionalService(@PathVariable Integer id, @Valid @RequestBody UpsertAdditionalServiceRequest request) {
        return ResponseEntity.ok(additionalServices.updateAdditionalService(id, request));
    }

    @DeleteMapping("/admin/additional-services/{id}")
    public ResponseEntity<?> deleteAdditionalService(@PathVariable Integer id) {
        additionalServices.deleteAdditionalService(id);
        return ResponseEntity.noContent().build();
    }
}
