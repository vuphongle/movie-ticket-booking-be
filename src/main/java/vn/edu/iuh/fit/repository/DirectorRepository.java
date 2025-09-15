package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Director;

import java.util.Optional;

public interface DirectorRepository extends JpaRepository<Director, Integer> {
}