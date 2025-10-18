package vn.edu.iuh.fit.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Integer> {
  List<Seat> findByAuditorium_Id(Integer auditoriumId);

  void deleteByAuditorium_Id(Integer id);

  List<Seat> findByAuditorium_IdAndRowIndex(Integer auditoriumId, Integer rowIndex);
}
