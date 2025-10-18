package vn.edu.iuh.fit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "images")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Image {
  @Id String id;

  String type;
  Double size;
  String url; // S3 URL của ảnh

  LocalDateTime createdAt;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "user_id")
  User user;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
