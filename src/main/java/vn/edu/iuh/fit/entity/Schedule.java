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
@Table(name = "schedules")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Schedule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @ManyToOne
  @JoinColumn(name = "movie_id")
  Movie movie;

  Date startDate;
  Date endDate;
}
