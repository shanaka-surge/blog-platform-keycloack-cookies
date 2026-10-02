package com.example.blog.repo;

import com.example.blog.model.Blog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {
  List<Blog> findAllByOrderByCreatedAtDesc();
  List<Blog> findByAuthorIdOrderByCreatedAtDesc(String authorId);
  void deleteByAuthorId(String authorId);
}
