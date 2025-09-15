package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Country;

public interface CountryRepository extends JpaRepository<Country, Integer> {
}