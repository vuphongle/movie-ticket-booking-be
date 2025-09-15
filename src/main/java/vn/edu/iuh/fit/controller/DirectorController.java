package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.service.DirectorService;

@Slf4j
@RestController
@RequestMapping("api/admin/directors")
@RequiredArgsConstructor
public class DirectorController {
    private final DirectorService directorService;

    @GetMapping
    public ResponseEntity<?> getAllDirectors() {
        return ResponseEntity.ok(directorService.getAllDirectors());
    }
}
