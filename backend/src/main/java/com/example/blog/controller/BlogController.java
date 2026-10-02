package com.example.blog.controller;

import com.example.blog.dto.BlogRequest;
import com.example.blog.dto.BlogResponse;
import com.example.blog.model.Blog;
import com.example.blog.repo.BlogRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

  private final BlogRepository repo;

  @Value("${app.admin-can-modify-blogs:false}")
  private boolean adminCanModify;

  /** GET all blogs (any logged-in user). */
  @GetMapping
  public List<BlogResponse> all(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
    return repo.findAllByOrderByCreatedAtDesc().stream()
        .map(b -> BlogResponse.from(b, canModify(b, jwt, auth))).toList();
  }

  /** GET only my blogs. */
  @GetMapping("/me")
  public List<BlogResponse> mine(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
    return repo.findByAuthorIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
        .map(b -> BlogResponse.from(b, canModify(b, jwt, auth))).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BlogResponse create(@Valid @RequestBody BlogRequest req, @AuthenticationPrincipal Jwt jwt) {
    Blog b = new Blog();
    b.setTitle(req.title());
    b.setContent(req.content());
    b.setAuthorId(jwt.getSubject());
    b.setAuthorName(jwt.getClaimAsString("preferred_username"));
    return BlogResponse.from(repo.save(b), true);
  }

  @PutMapping("/{id}")
  public BlogResponse update(@PathVariable Long id, @Valid @RequestBody BlogRequest req,
                             @AuthenticationPrincipal Jwt jwt, Authentication auth) {
    Blog b = findAndCheck(id, jwt, auth);
    b.setTitle(req.title());
    b.setContent(req.content());
    b.setUpdatedAt(Instant.now());
    return BlogResponse.from(repo.save(b), true);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
    repo.delete(findAndCheck(id, jwt, auth));
  }

  // ---- ownership rules live here ----
  private Blog findAndCheck(Long id, Jwt jwt, Authentication auth) {
    Blog b = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found"));
    if (!canModify(b, jwt, auth)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only change your own blogs");
    }
    return b;
  }

  private boolean canModify(Blog b, Jwt jwt, Authentication auth) {
    boolean owner = b.getAuthorId().equals(jwt.getSubject());
    boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    return owner || (adminCanModify && admin);
  }
}
