package vn.edu.iuh.fit.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.service.BlogService;

@Slf4j
@RestController
@RequestMapping("api/public/blogs")
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;

    @GetMapping()
    public ResponseEntity<?> getAllBlogs(@RequestParam(required = false) String type,
                                         @RequestParam(required = false, defaultValue = "1") Integer page,
                                         @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(blogService.getAllBlogs(type, page, limit));
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getBlogsLatest(@RequestParam(required = false) String type,
                                            @RequestParam(required = false, defaultValue = "1") Integer page,
                                            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(blogService.getBlogsLatest(type, page, limit));
    }

    @GetMapping("/most-view")
    public ResponseEntity<?> getMostViewBlogs(@RequestParam(required = false) String type,
                                              @RequestParam(required = false, defaultValue = "5") Integer limit) {
        return ResponseEntity.ok(blogService.getMostViewBlogs(type, limit));
    }
}