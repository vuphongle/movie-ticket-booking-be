package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.model.request.UpdatePasswordRequest;
import vn.edu.iuh.fit.model.request.UpdateProfileUserRequest;
import vn.edu.iuh.fit.model.response.UploadResponse;
import vn.edu.iuh.fit.service.S3Service;
import vn.edu.iuh.fit.service.UserService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final S3Service s3Service;

    @PutMapping("/users/update-password")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/update-profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileUserRequest request) {
        User user = userService.updateProfile(request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/upload-avatar")
    public ResponseEntity<UploadResponse> uploadUserAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String url = s3Service.uploadUserAvatar(file);
            UploadResponse response = new UploadResponse(
                    url,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi upload avatar: {}", e.getMessage());
            UploadResponse errorResponse = new UploadResponse();
            errorResponse.setMessage("Upload thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
