package vn.edu.iuh.fit.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
  List<Review> findByMovie_Id(Integer movieId);

  Page<Review> findByMovie_Id(Integer movieId, Pageable pageable);
}
