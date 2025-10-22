package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.service.ImageService;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImageController {
  private final ImageService imageService;

  @GetMapping("/admin/images")
  public ResponseEntity<?> getAllImage() {
    return ResponseEntity.ok(imageService.getAllImage());
  }

  @PostMapping("/admin/images")
  public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
    return new ResponseEntity<>(imageService.uploadImage(file), HttpStatus.CREATED);
  }

  //  @GetMapping("/public/images/{id}")
  //  public ResponseEntity<?> readImage(@PathVariable String id) {
  //    Image image = imageService.getImageById(id);
  //    if (image == null) {
  //      return ResponseEntity.notFound().build();
  //    }
  //    return ResponseEntity.ok()
  //        .contentType(MediaType.parseMediaType(image.getType()))
  //        .body(imageService.getImageData(image));
  //  }

  @DeleteMapping("/admin/images/{id}")
  public ResponseEntity<?> deleteImage(@PathVariable String id) {
    imageService.deleteImage(id);
    return ResponseEntity.ok().build();
  }
}
