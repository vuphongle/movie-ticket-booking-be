package vn.edu.iuh.fit.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.model.dto.BlogDto;
import vn.edu.iuh.fit.model.enums.BlogType;
import vn.edu.iuh.fit.repository.BlogRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {
  private final BlogRepository blogRepository;

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
}
