package vn.edu.iuh.fit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.UserRole;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  String name;

  @Temporal(TemporalType.DATE)
  Date dob;

  @Column(unique = true)
  String email;

  String phone;

  @JsonIgnore
  @Column(nullable = false)
  String password;

  String avatar;

  @Enumerated(EnumType.STRING)
  UserRole role;

  Boolean enabled;

  Date createdAt;
  Date updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id")
  Cinema cinema;

  @PrePersist
  public void prePersist() {
    createdAt = new Date();
    updatedAt = createdAt;

    if (role == null) {
      role = UserRole.USER;
    }

    if (enabled == null) {
      enabled = false;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = new Date();
  }
}
