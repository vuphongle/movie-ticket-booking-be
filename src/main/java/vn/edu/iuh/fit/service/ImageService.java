package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.entity.Image;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.model.response.ImageResponse;
import vn.edu.iuh.fit.repository.ImageRepository;
import vn.edu.iuh.fit.security.SecurityUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
    private final String uploadDir = "image_uploads";
    private final ImageRepository imageRepository;
    private final S3Service s3Service;


    public List<ImageResponse> getAllImage() {
        User user = SecurityUtils.getCurrentUserLogin();
        List<Image> images = imageRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());

        return images.stream()
                .map(image -> new ImageResponse(image.getId(), image.getUrl()))
                .toList();
    }

    public ImageResponse uploadImage(MultipartFile file) {
        User user = SecurityUtils.getCurrentUserLogin();
        validateFile(file);

        try {
            // Upload ảnh lên S3 và lấy URL
            String s3Url = s3Service.uploadImage(file);

            String imageId = UUID.randomUUID().toString();

            Image image = Image.builder()
                    .id(imageId)
                    .type(file.getContentType())
                    .size(extractSize(file))
                    .url(s3Url)
                    .user(user)
                    .build();

            imageRepository.save(image);

            return ImageResponse.builder()
                    .id(imageId)
                    .url(s3Url)
                    .build();
        } catch (Exception e) {
            log.error("Cannot upload file to S3: " + e.getMessage());
            throw new RuntimeException("Cannot upload file: " + e.getMessage());
        }
    }
    public ImageResponse uploadQRCodeImage(byte[] data) {
        String imageId = UUID.randomUUID().toString();
        Path rootPath = Paths.get(uploadDir);
        Path filePath = rootPath.resolve(imageId);

        try {
            Files.write(filePath, data);

            // Tính toán kích thước file theo MB
            double sizeInMB = BigDecimal.valueOf(data.length / 1024.0 / 1024.0)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            Image image = Image.builder()
                    .id(imageId)
                    .type("image/png")
                    .size(sizeInMB)
                    .build();

            imageRepository.save(image);

            String url = "/api/public/images/" + image.getId();
            return ImageResponse.builder()
                    .id(imageId)
                    .url(url)
                    .build();
        } catch (IOException e) {
            log.error("Cannot upload file: " + filePath);
            log.error(e.getMessage());
            throw new RuntimeException("Cannot upload file: " + filePath);
        }
    }


    private void validateFile(MultipartFile file) {
        // Kiểm tra tên file
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new BadRequestException("File name không được để trống");
        }

        String fileExtension = getFileExtensiton(fileName);
        if (!checkFileExtension(fileExtension)) {
            throw new BadRequestException("File không đúng định dạng");
        }

        // Kiểm tra dung lượng file (<= 2MB)
        if (extractSize(file) > 2.0) {
            throw new BadRequestException("File không được vượt quá 2MB");
        }
    }

    private String getFileExtensiton(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        return fileName.substring(lastIndexOf + 1);
    }

    private boolean checkFileExtension(String fileExtension) {
        List<String> extensions = new ArrayList<>(List.of("png", "jpg", "jpeg"));
        return extensions.contains(fileExtension.toLowerCase());
    }

    // Tính toán kích thước của file
    public double extractSize(MultipartFile file) {
        long sizeInBytes = file.getSize();

        // làm tròn 2 dấu phay động
        return Math.round((double) sizeInBytes / (1024 * 1024) * 100) / 100.0;
    }

    public Image getImageById(String id) {
        return imageRepository.findById(id).orElse(null);
    }

    public byte[] getImageData(Image image) {
        Path rootPath = Paths.get(uploadDir);
        Path filePath = rootPath.resolve(image.getId());

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Cannot read file: {}", filePath);
            log.error(e.getMessage());
            throw new RuntimeException("Cannot read file: " + filePath);
        }
    }

    public void deleteImage(String imageId) {
        User user = SecurityUtils.getCurrentUserLogin();
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy ảnh"));
        
        // Kiểm tra quyền sở hữu
        if (!image.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Bạn không có quyền xóa ảnh này");
        }

        // Xóa ảnh khỏi S3
        try {
            s3Service.deleteFileByUrl(image.getUrl());
        } catch (Exception e) {
            log.warn("Không thể xóa file từ S3: " + e.getMessage());
        }

        // Xóa record khỏi database
        imageRepository.delete(image);
    }
}
