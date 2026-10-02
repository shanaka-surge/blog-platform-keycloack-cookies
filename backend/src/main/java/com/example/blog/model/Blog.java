package com.example.blog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Blog {
  @Id @GeneratedValue private Long id;
  private String title;
  @Column(length = 10000) private String content;
  private String authorId;    // Keycloak user id (JWT "sub")
  private String authorName;  // JWT "preferred_username"
  private Instant createdAt = Instant.now();
  private Instant updatedAt;
}
