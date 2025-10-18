package vn.edu.iuh.fit.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
  private String url;
  private String fileName;
  private Long fileSize;
  private String contentType;
  private String message;

  public UploadResponse(String url, String fileName, Long fileSize, String contentType) {
    this.url = url;
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.contentType = contentType;
    this.message = "Upload thành công";
  }
}
