package com.example.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {
  private UserDtos() {}

  public record CreateUser(@NotBlank String username, @NotBlank @Email String email,
                           @NotBlank @Size(min = 6) String password, boolean admin) {}

  /** password is optional on update: blank means "keep the current one". */
  public record UpdateUser(@NotBlank @Email String email, Boolean enabled, String password) {}

  public record UserView(String id, String username, String email, boolean enabled) {}
}
