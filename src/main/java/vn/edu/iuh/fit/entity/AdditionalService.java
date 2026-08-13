package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.AdditionalServiceType;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "additional_services")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdditionalService {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @Column(nullable = false)
  String name;

  @Column(length = 1000)
  String description;

  String thumbnail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  AdditionalServiceType type; // SINGLE hoặc COMBO

  // Chỉ sử dụng khi type = SINGLE
  Integer productId; // Tham chiếu đến Product

  // Chỉ sử dụng khi type = SINGLE
  Integer defaultQuantity; // Số lượng mặc định

  @Builder.Default Boolean status = true; // Trạng thái hiển thị

  @Temporal(TemporalType.TIMESTAMP)
  Date createdAt;

  @Temporal(TemporalType.TIMESTAMP)
  Date updatedAt;

  @Temporal(TemporalType.TIMESTAMP)
  Date publishedAt;

  // Relationship với AdditionalServiceItem (cho COMBO)
  @OneToMany(mappedBy = "additionalService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  List<AdditionalServiceItem> items;

  @PrePersist
  protected void onCreate() {
    createdAt = new Date();
    updatedAt = new Date();
    if (status) {
      publishedAt = new Date();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
    if (status) {
      publishedAt = new Date();
    } else {
      publishedAt = null;
    }
  }
}
