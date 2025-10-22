package vn.edu.iuh.fit.service;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

  private final S3Client s3Client;

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  @Value("${aws.s3.region}")
  private String region;

  /** Upload file lên S3 và trả về URL public */
  public String uploadFile(MultipartFile file, String folder) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File không được rỗng");
    }

    // Validate file type
    String contentType = file.getContentType();
    if (contentType == null || !isValidImageType(contentType)) {
      throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, GIF, WebP)");
    }

    // Validate file size (max 10MB)
    if (file.getSize() > 10 * 1024 * 1024) {
      throw new IllegalArgumentException("Kích thước file không được vượt quá 10MB");
    }

    try {
      // Tạo key unique cho file
      String fileName = generateFileName(file.getOriginalFilename());
      String key = folder.isEmpty() ? fileName : folder + "/" + fileName;

      // Upload file lên S3
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .contentType(contentType)
              .contentLength(file.getSize())
              .build();

      s3Client.putObject(
          putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      // Trả về URL public
      String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);

      log.info("File uploaded successfully: {}", fileUrl);
      return fileUrl;

    } catch (S3Exception e) {
      log.error("Lỗi khi upload file lên S3: {}", e.getMessage());
      throw new RuntimeException("Không thể upload file lên S3: " + e.getMessage());
    } catch (IOException e) {
      log.error("Lỗi khi đọc file: {}", e.getMessage());
      throw new RuntimeException("Không thể đọc file: " + e.getMessage());
    }
  }

  /** Upload ảnh cho movies */
  public String uploadMovieImage(MultipartFile file) {
    return uploadFile(file, "movies");
  }

  /** Upload ảnh cho users (avatar) */
  public String uploadUserAvatar(MultipartFile file) {
    return uploadFile(file, "users");
  }

  /** Upload ảnh cho blogs */
  public String uploadBlogImage(MultipartFile file) {
    return uploadFile(file, "blogs");
  }

  /** Upload ảnh chung */
  public String uploadImage(MultipartFile file) {
    return uploadFile(file, "images");
  }

  /** Kiểm tra loại file có hợp lệ không */
  private boolean isValidImageType(String contentType) {
    return contentType.equals("image/jpeg")
        || contentType.equals("image/jpg")
        || contentType.equals("image/png")
        || contentType.equals("image/gif")
        || contentType.equals("image/webp");
  }

  /** Tạo tên file unique */
  private String generateFileName(String originalFileName) {
    String extension = "";
    if (originalFileName != null && originalFileName.contains(".")) {
      extension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }
    return UUID.randomUUID().toString() + extension;
  }

  /** Xóa file từ S3 bằng URL */
  public void deleteFileByUrl(String fileUrl) {
    try {
      // Extract key từ URL
      // URL format: https://bucket-name.s3.region.amazonaws.com/folder/filename
      String key = extractKeyFromUrl(fileUrl);

      s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));

      log.info("File deleted successfully: {}", fileUrl);
    } catch (S3Exception e) {
      log.error("Lỗi khi xóa file từ S3: {}", e.getMessage());
      throw new RuntimeException("Không thể xóa file từ S3: " + e.getMessage());
    }
  }

  /** Trích xuất key từ S3 URL */
  private String extractKeyFromUrl(String fileUrl) {
    // URL format: https://bucket-name.s3.region.amazonaws.com/folder/filename
    String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
    if (fileUrl.startsWith(baseUrl)) {
      return fileUrl.substring(baseUrl.length());
    }
    throw new IllegalArgumentException("URL không hợp lệ: " + fileUrl);
  }

  public String uploadImage(String fileName, byte[] data, String contentType) {
    try {
      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(fileName)
              .contentType(contentType)
              .contentLength((long) data.length)
              .build();

      s3Client.putObject(request, RequestBody.fromBytes(data));

      return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
    } catch (S3Exception e) {
      log.error("Lỗi khi upload file lên S3: {}", e.getMessage());
      throw new RuntimeException("Không thể upload file lên S3: " + e.getMessage());
    }
  }
}
