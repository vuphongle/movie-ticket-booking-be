package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.RedemptionConfirmRequest;
import vn.edu.iuh.fit.service.RedemptionService;

@Slf4j
@RestController
@RequestMapping("api/redemptions")
@RequiredArgsConstructor
public class RedemptionController {
    private final RedemptionService redemptionService;

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmRedemption(@Valid @RequestBody RedemptionConfirmRequest request) {
        redemptionService.confirmRedemption(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revert")
    public ResponseEntity<?> revertRedemption(@Valid @RequestBody RedemptionConfirmRequest request) {
        redemptionService.revertRedemption(request);
        return ResponseEntity.ok().build();
    }
}