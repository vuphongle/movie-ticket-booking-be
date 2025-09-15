package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.service.ActorService;

@Slf4j
@RestController
@RequestMapping("api/admin/actors")
@RequiredArgsConstructor
public class ActorController {
    private final ActorService actorService;

    @GetMapping
    public ResponseEntity<?> getAllActors() {
        return ResponseEntity.ok(actorService.getAllActors());
    }
}
