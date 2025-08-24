package vn.edu.iuh.fit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByMovie_Id(Integer movieId);

    Page<Review> findByMovie_Id(Integer movieId, Pageable pageable);
}
