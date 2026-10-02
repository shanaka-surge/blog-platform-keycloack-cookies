package com.example.blog.dto;

import com.example.blog.model.Blog;
import java.time.Instant;

public record BlogResponse(Long id, String title, String content, String authorId,
                           String authorName, Instant createdAt, Instant updatedAt,
                           boolean canModify) {
  public static BlogResponse from(Blog b, boolean canModify) {
    return new BlogResponse(b.getId(), b.getTitle(), b.getContent(), b.getAuthorId(),
        b.getAuthorName(), b.getCreatedAt(), b.getUpdatedAt(), canModify);
  }
}
