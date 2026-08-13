package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "additional_service_items")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdditionalServiceItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "additional_service_id", nullable = false)
  AdditionalService additionalService;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  Product product;

  @Column(nullable = false)
  Integer quantity; // Số lượng sản phẩm trong combo (>=1)

  // Constructor for convenience
  public AdditionalServiceItem(
      AdditionalService additionalService, Product product, Integer quantity) {
    this.additionalService = additionalService;
    this.product = product;
    this.quantity = quantity;
  }
}
