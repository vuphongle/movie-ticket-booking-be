package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.model.response.UploadResponse;
import vn.edu.iuh.fit.service.S3Service;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UploadController {

    private final S3Service s3Service;

    /**
     * Upload ảnh chung
     */
    @PostMapping("/image")
    public ResponseEntity<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = s3Service.uploadImage(file);
            UploadResponse response = new UploadResponse(
                    url,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi upload ảnh: {}", e.getMessage());
            UploadResponse errorResponse = new UploadResponse();
            errorResponse.setMessage("Upload thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Upload ảnh cho movie
     */
    @PostMapping("/movie-image")
    public ResponseEntity<UploadResponse> uploadMovieImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = s3Service.uploadMovieImage(file);
            UploadResponse response = new UploadResponse(
                    url,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi upload ảnh movie: {}", e.getMessage());
            UploadResponse errorResponse = new UploadResponse();
            errorResponse.setMessage("Upload thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Upload ảnh cho blog
     */
    @PostMapping("/blog-image")
    public ResponseEntity<UploadResponse> uploadBlogImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = s3Service.uploadBlogImage(file);
            UploadResponse response = new UploadResponse(
                    url,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi upload ảnh blog: {}", e.getMessage());
            UploadResponse errorResponse = new UploadResponse();
            errorResponse.setMessage("Upload thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Upload multiple images
     */
    @PostMapping("/multiple-images")
    public ResponseEntity<?> uploadMultipleImages(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files.length > 10) {
                return ResponseEntity.badRequest()
                        .body("Chỉ được upload tối đa 10 ảnh cùng lúc");
            }

            UploadResponse[] responses = new UploadResponse[files.length];
            for (int i = 0; i < files.length; i++) {
                String url = s3Service.uploadImage(files[i]);
                responses[i] = new UploadResponse(
                        url,
                        files[i].getOriginalFilename(),
                        files[i].getSize(),
                        files[i].getContentType()
                );
            }
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Lỗi khi upload multiple images: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Upload thất bại: " + e.getMessage());
        }
    }
}
