package vn.edu.iuh.fit.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.AdditionalService;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Integer> {
  List<AdditionalService> findByStatus(Boolean status);
}
