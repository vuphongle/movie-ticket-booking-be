package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.TokenType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "token_confirms")
public class TokenConfirm {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  private TokenType type;

  private Date createdDate;
  private Date confirmedDate;
  private Date expiryDate;

  @PrePersist
  public void prePersist() {
    this.createdDate = new Date();
  }
}
