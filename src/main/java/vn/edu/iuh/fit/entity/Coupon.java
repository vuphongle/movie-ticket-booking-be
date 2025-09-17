package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "coupons")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(unique = true, nullable = false)
    String code;
    
    @Column(nullable = false)
    String name;
    
    String description;
    
    @Column(nullable = false)
    Boolean status;

    @Column(name = "start_date", nullable = false)
    Date startDate;

    @Column(name = "end_date", nullable = false)
    Date endDate;

    @Column(name = "created_at")
    Date createdAt;

    @Column(name = "updated_at")
    Date updatedAt;

    @PrePersist
    protected void onCreate() {
        if (code != null) {
            code = code.toUpperCase(); // Tự động chuyển code thành uppercase
        }
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        if (code != null) {
            code = code.toUpperCase(); // Tự động chuyển code thành uppercase
        }
        updatedAt = new Date();
    }
}
