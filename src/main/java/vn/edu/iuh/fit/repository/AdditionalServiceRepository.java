package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.AdditionalService;

import java.util.List;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Integer> {
    List<AdditionalService> findByStatus(Boolean status);
}