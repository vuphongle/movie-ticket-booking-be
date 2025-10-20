package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import vn.edu.iuh.fit.model.enums.OrderStatus;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "orders")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {
  @Id Integer id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  User user;

  @ManyToOne
  @JoinColumn(name = "show_id")
  Showtime showtime;

  @Enumerated(EnumType.STRING)
  OrderStatus status;

  @Builder.Default
  @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
  Integer discount = 0;

  @Transient Integer tempPrice;

  @Transient Integer discountPrice;

  @Transient Integer totalPrice;

  @Builder.Default
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  @Fetch(FetchMode.SUBSELECT)
  List<OrderTicketItem> ticketItems = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  @Fetch(FetchMode.SUBSELECT)
  List<OrderServiceItem> serviceItems = new ArrayList<>();

  String qrCodePath;

  private String pdfPath;

  LocalDateTime createdAt;
  LocalDateTime updatedAt;

  // Lưu lại request để tái sử dụng khi cần
  @Column(columnDefinition = "TEXT")
  private String requestSnapshot;

  public int getTempPrice() {
    if (ticketItems == null || serviceItems == null) {
      return 0;
    }
    Integer ticketPrice =
        ticketItems.stream().map(OrderTicketItem::getPrice).reduce(0, Integer::sum);
    Integer servicePrice =
        serviceItems.stream()
            .map(serviceItems -> serviceItems.getPrice() * serviceItems.getQuantity())
            .reduce(0, Integer::sum);
    return ticketPrice + servicePrice;
  }

  public int getDiscountPrice() {
    // Ensure discount is never null
    Integer discountValue = (discount != null) ? discount : 0;
    return discountValue;
  }

  public int getTotalPrice() {
    return getTempPrice() - getDiscountPrice();
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    // Ensure discount is never null
    if (discount == null) {
      discount = 0;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
    // Ensure discount is never null
    if (discount == null) {
      discount = 0;
    }
  }

  public void addTicketItem(OrderTicketItem orderTicketItem) {
    orderTicketItem.setOrder(this);
    ticketItems.add(orderTicketItem);
  }

  public void addServiceItem(OrderServiceItem orderServiceItem) {
    orderServiceItem.setOrder(this);
    serviceItems.add(orderServiceItem);
  }
}
