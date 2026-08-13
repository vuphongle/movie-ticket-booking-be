package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "price_lists")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceList {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @Column(nullable = false)
  String name; // Tên bảng giá

  @Builder.Default Boolean status = true; // Trạng thái kích hoạt

  @Temporal(TemporalType.TIMESTAMP)
  Date validFrom; // Ngày bắt đầu hiệu lực

  @Temporal(TemporalType.TIMESTAMP)
  Date validTo; // Ngày kết thúc hiệu lực

  @Temporal(TemporalType.TIMESTAMP)
  Date createdAt;

  @Temporal(TemporalType.TIMESTAMP)
  Date updatedAt;

  // Relationship với PriceItem
  @OneToMany(mappedBy = "priceList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  List<PriceItem> priceItems;

  @PrePersist
  protected void onCreate() {
    createdAt = new Date();
    updatedAt = new Date();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }

  // Helper method để kiểm tra bảng giá có hiệu lực tại thời điểm hiện tại không
  public boolean isValidAt(Date checkDate) {
    if (!status) return false;
    if (validFrom != null && checkDate.before(validFrom)) return false;
    if (validTo != null && checkDate.after(validTo)) return false;
    return true;
  }
}
