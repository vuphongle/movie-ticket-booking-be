package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "products")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @Column(unique = true, nullable = false)
  String sku; // Mã SKU duy nhất

  @Column(nullable = false)
  String name; // Tên sản phẩm

  @Column(length = 1000)
  String description; // Mô tả sản phẩm

  String unit; // Đơn vị (box, bottle, piece, etc.)

  Integer quantity; // Số lượng tồn kho

  String thumbnail; // Ảnh đại diện

  @Builder.Default Boolean status = true; // Trạng thái hiển thị

  @Temporal(TemporalType.TIMESTAMP)
  Date createdAt;

  @Temporal(TemporalType.TIMESTAMP)
  Date updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = new Date();
    updatedAt = new Date();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }
}
