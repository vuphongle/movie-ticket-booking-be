package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entity.Image;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, String> {
    @Query("select i from Image i where i.user.id = ?1 order by i.createdAt DESC")
    List<Image> findByUser_IdOrderByCreatedAtDesc(Integer id);
}