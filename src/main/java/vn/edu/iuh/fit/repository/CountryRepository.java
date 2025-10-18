package vn.edu.iuh.fit.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Country;

public interface CountryRepository extends JpaRepository<Country, Integer> {
  Optional<Country> findByName(String name);
}
