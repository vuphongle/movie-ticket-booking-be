package vn.edu.iuh.fit.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entity.Cinema;

public interface CinemaRepository extends JpaRepository<Cinema, Integer> {
  @Query("SELECT c.name FROM Cinema c")
  List<String> findAllCinemaNames();

  @Query("SELECT c.address FROM Cinema c")
  List<String> findAllAddresses();

  Optional<Cinema> findByNameIgnoreCase(String name);
}
