package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.model.request.UpsertBlogRequest;
import vn.edu.iuh.fit.service.BlogService;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class BlogController {
  private final BlogService blogService;

  @GetMapping("/public/blogs")
  public ResponseEntity<?> getAllBlogs(
      @RequestParam(required = false) String type,
      @RequestParam(required = false, defaultValue = "1") Integer page,
      @RequestParam(required = false, defaultValue = "10") Integer limit) {
    return ResponseEntity.ok(blogService.getAllBlogs(type, page, limit));
  }

  @GetMapping("/public/blogs/latest")
  public ResponseEntity<?> getBlogsLatest(
      @RequestParam(required = false) String type,
      @RequestParam(required = false, defaultValue = "1") Integer page,
      @RequestParam(required = false, defaultValue = "10") Integer limit) {
    return ResponseEntity.ok(blogService.getBlogsLatest(type, page, limit));
  }

  @GetMapping("/public/blogs/most-view")
  public ResponseEntity<?> getMostViewBlogs(
      @RequestParam(required = false) String type,
      @RequestParam(required = false, defaultValue = "5") Integer limit) {
    return ResponseEntity.ok(blogService.getMostViewBlogs(type, limit));
  }

  @GetMapping("/admin/blogs")
  public ResponseEntity<?> getAllBlog() {
    return ResponseEntity.ok(blogService.getAllBlog());
  }

  @GetMapping("/admin/blogs/own-blogs")
  public ResponseEntity<?> getOwnBlogs() {
    return ResponseEntity.ok(blogService.getOwnBlogs());
  }

  @PostMapping("/admin/blogs")
  public ResponseEntity<?> createBlog(@RequestBody UpsertBlogRequest request) {
    return new ResponseEntity<>(blogService.createBlog(request), HttpStatus.CREATED);
  }

  @GetMapping("/admin/blogs/{id}")
  public ResponseEntity<?> getBlogById(@PathVariable Integer id) {
    return ResponseEntity.ok(blogService.getBlogById(id));
  }

  @PutMapping("/admin/blogs/{id}")
  public ResponseEntity<?> updateBlog(
      @PathVariable Integer id, @RequestBody UpsertBlogRequest request) {
    return ResponseEntity.ok(blogService.updateBlog(id, request));
  }

  @DeleteMapping("/admin/blogs/{id}")
  public ResponseEntity<?> deleteBlog(@PathVariable Integer id) {
    blogService.deleteBlog(id);
    return ResponseEntity.noContent().build();
  }
}
