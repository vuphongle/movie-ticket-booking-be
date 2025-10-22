package vn.edu.iuh.fit.service;

import com.github.slugify.Slugify;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.Blog;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.dto.BlogDto;
import vn.edu.iuh.fit.model.enums.BlogType;
import vn.edu.iuh.fit.model.request.UpsertBlogRequest;
import vn.edu.iuh.fit.repository.BlogRepository;
import vn.edu.iuh.fit.security.SecurityUtils;
import vn.edu.iuh.fit.utils.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {
  private final BlogRepository blogRepository;
  private final Slugify slugify;

  public Page<BlogDto> getAllBlogs(String type, Integer page, Integer limit) {
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
    if (type == null || type.isEmpty() || type.equals("all")) {
      return blogRepository.findByStatus(true, pageable);
    }
    return blogRepository.findByTypeAndStatus(BlogType.valueOf(type), true, pageable);
  }

  public Page<BlogDto> getBlogsLatest(String type, Integer page, Integer limit) {
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
    Page<BlogDto> pageData;
    if (type == null || type.isEmpty() || type.equals("all")) {
      pageData = blogRepository.findByStatus(true, pageable);
    } else {
      pageData = blogRepository.findByTypeAndStatus(BlogType.valueOf(type), true, pageable);
    }
    return pageData;
  }

  public List<BlogDto> getMostViewBlogs(String type, Integer limit) {
    LocalDateTime start = LocalDateTime.now().minusMonths(2);
    LocalDateTime end = LocalDateTime.now();

    List<BlogDto> blogs;
    if (type == null || type.isEmpty() || type.equals("all")) {
      blogs = blogRepository.findMostViewBlog(start, end);
    } else {
      blogs = blogRepository.findMostViewBlogByType(BlogType.valueOf(type), start, end);
    }

    if (blogs.size() > limit) {
      return blogs.subList(0, limit);
    }
    return blogs;
  }

  public List<Blog> getAllBlog() {
    return blogRepository.findByOrderByCreatedAtDesc();
  }

  public List<Blog> getOwnBlogs() {
    User user = SecurityUtils.getCurrentUserLogin();
    return blogRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
  }

  @Transactional
  public Blog createBlog(UpsertBlogRequest request) {
    User user = SecurityUtils.getCurrentUserLogin();

    Blog blog =
        Blog.builder()
            .title(request.getTitle())
            .slug(slugify.slugify(request.getTitle()))
            .content(request.getContent())
            .description(request.getDescription())
            .thumbnail(StringUtils.generateLinkImage(request.getTitle()))
            .status(request.getStatus())
            .type(request.getType())
            .user(user)
            .build();
    return blogRepository.save(blog);
  }

  public Blog getBlogById(Integer id) {
    return blogRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blog có id = " + id));
  }

  @Transactional
  public Blog updateBlog(Integer id, UpsertBlogRequest request) {
    Blog blog =
        blogRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blog có id = " + id));

    blog.setTitle(request.getTitle());
    blog.setSlug(slugify.slugify(request.getTitle()));
    blog.setDescription(request.getDescription());
    blog.setContent(request.getContent());
    blog.setStatus(request.getStatus());
    blog.setType(request.getType());
    blog.setThumbnail(request.getThumbnail());
    return blogRepository.save(blog);
  }

  @Transactional
  public void deleteBlog(Integer id) {
    Blog blog =
        blogRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blog có id = " + id));
    blogRepository.delete(blog);
  }
}
