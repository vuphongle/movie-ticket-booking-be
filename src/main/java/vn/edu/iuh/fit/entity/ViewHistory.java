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
@Table(name = "view_histories")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViewHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "blog_id")
  Blog blog;

  @Column(name = "viewed_at")
  LocalDateTime viewedAt;

  @PrePersist
  public void prePersist() {
    viewedAt = LocalDateTime.now();
  }
}
