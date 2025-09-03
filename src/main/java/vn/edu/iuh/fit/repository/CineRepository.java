package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entity.Cinema;

import java.util.List;

public interface CineRepository extends JpaRepository<Cinema, Long> {
    @Query("SELECT c.name FROM Cinema c")
    List<String> findAllCinemaNames();

    @Query("SELECT c.address FROM Cinema c")
    List<String> findAllAddresses();
}
