package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.service.SeatTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SeatTypeController {
    
    private final SeatTypeService seatTypeService;
    
    @GetMapping("/seat-types")
    public ResponseEntity<List<SeatTypeService.SeatTypeOption>> getSeatTypeOptions() {
        List<SeatTypeService.SeatTypeOption> options = seatTypeService.getAllSeatTypeOptions();
        return ResponseEntity.ok(options);
    }
}