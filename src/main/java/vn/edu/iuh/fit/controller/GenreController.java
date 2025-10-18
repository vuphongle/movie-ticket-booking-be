package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertGenreRequest;
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

  @PostMapping("/admin/genres")
  public ResponseEntity<?> createGenre(@Valid @RequestBody UpsertGenreRequest request) {
    return new ResponseEntity<>(genreService.saveGenre(request), HttpStatus.CREATED);
  }

  @PutMapping("/admin/genres/{id}")
  public ResponseEntity<?> updateGenre(
      @PathVariable Integer id, @Valid @RequestBody UpsertGenreRequest request) {
    return ResponseEntity.ok(genreService.updateGenre(id, request));
  }

  @DeleteMapping("/admin/genres/{id}")
  public ResponseEntity<?> deleteGenre(@PathVariable Integer id) {
    genreService.deleteGenre(id);
    return ResponseEntity.noContent().build();
  }
}
