package vn.edu.iuh.fit.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entity.Image;

public interface ImageRepository extends JpaRepository<Image, String> {
  @Query("select i from Image i where i.user.id = ?1 order by i.createdAt DESC")
  List<Image> findByUser_IdOrderByCreatedAtDesc(Integer id);

  List<Image> findByUser_Id(Integer id);
}
