package vn.edu.iuh.fit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entity.Blog;
import vn.edu.iuh.fit.model.dto.BlogDto;
import vn.edu.iuh.fit.model.dto.BlogViewDto;
import vn.edu.iuh.fit.model.enums.BlogType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Integer> {
    @Query("select new vn.edu.iuh.fit.model.dto.BlogDto(b.id, b.title, b.slug, b.description, b.thumbnail, b.publishedAt) from Blog b where b.status = ?1")
    Page<BlogDto> findByStatus(Boolean status, Pageable pageable);

    @Query("select new vn.edu.iuh.fit.model.dto.BlogDto(b.id, b.title, b.slug, b.description, b.thumbnail, b.publishedAt) from Blog b where b.type = ?1 and b.status = ?2")
    Page<BlogDto> findByTypeAndStatus(BlogType type, boolean b, Pageable pageable);

    // get most view blog in 2 months latest. Join to view history to get view count -> return blog
    @Query("select new vn.edu.iuh.fit.model.dto.BlogDto(b.id, b.title, b.slug, b.description, b.thumbnail, b.publishedAt) from Blog b join ViewHistory vh on b.id = vh.blog.id where b.status = true and vh.viewedAt between ?1 and ?2 group by b.id order by count(vh.id) desc")
    List<BlogDto> findMostViewBlog(LocalDateTime start, LocalDateTime end);


    @Query("select new vn.edu.iuh.fit.model.dto.BlogDto(b.id, b.title, b.slug, b.description, b.thumbnail, b.publishedAt) from Blog b join ViewHistory vh on b.id = vh.blog.id where b.type = ?1 and b.status = true and vh.viewedAt between ?2 and ?3 group by b.id order by count(vh.id) desc")
    List<BlogDto> findMostViewBlogByType(BlogType blogType, LocalDateTime start, LocalDateTime end);
}