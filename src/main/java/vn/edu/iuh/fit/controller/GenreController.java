package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.service.GenreService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class GenreController {
    private final GenreService genreService;

    @GetMapping("/admin/genres")
    public ResponseEntity<?> getAllGenresByAdmin() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }
}
