package com.example.blog.controller;

import com.example.blog.dto.UserDtos.CreateUser;
import com.example.blog.dto.UserDtos.UpdateUser;
import com.example.blog.dto.UserDtos.UserView;
import com.example.blog.repo.BlogRepository;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** User CRUD for admins. Users live in Keycloak; we call its Admin API. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final Keycloak keycloak;
  private final BlogRepository blogs;

  @Value("${keycloak.realm}")
  private String realm;

  private UsersResource users() { return keycloak.realm(realm).users(); }

  private static UserView view(UserRepresentation u) {
    return new UserView(u.getId(), u.getUsername(), u.getEmail(), Boolean.TRUE.equals(u.isEnabled()));
  }

  private static CredentialRepresentation password(String value) {
    var c = new CredentialRepresentation();
    c.setType(CredentialRepresentation.PASSWORD);
    c.setValue(value);
    c.setTemporary(false);
    return c;
  }

  @GetMapping
  public List<UserView> list() {
    return users().list(0, 200).stream().map(AdminUserController::view).toList();
  }

  @GetMapping("/{id}")
  public UserView get(@PathVariable String id) {
    return view(users().get(id).toRepresentation());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserView create(@Valid @RequestBody CreateUser req) {
    var u = new UserRepresentation();
    u.setUsername(req.username());
    u.setEmail(req.email());
    u.setEmailVerified(true);
    u.setEnabled(true);
    u.setCredentials(List.of(password(req.password())));

    try (Response r = users().create(u)) {
      if (r.getStatus() == 409) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
      }
      String id = CreatedResponseUtil.getCreatedId(r);
      // New users get USER via the realm's default role; add ADMIN if requested.
      if (req.admin()) {
        var adminRole = keycloak.realm(realm).roles().get("ADMIN").toRepresentation();
        users().get(id).roles().realmLevel().add(List.of(adminRole));
      }
      return view(users().get(id).toRepresentation());
    }
  }

  @PutMapping("/{id}")
  public UserView update(@PathVariable String id, @Valid @RequestBody UpdateUser req) {
    var res = users().get(id);
    var u = res.toRepresentation();
    u.setEmail(req.email());
    if (req.enabled() != null) u.setEnabled(req.enabled());
    res.update(u);
    if (req.password() != null && !req.password().isBlank()) {
      res.resetPassword(password(req.password()));
    }
    return view(res.toRepresentation());
  }

  @DeleteMapping("/{id}")
  @Transactional
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    try (Response r = users().delete(id)) {
      if (r.getStatus() == 404) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    blogs.deleteByAuthorId(id); // don't leave orphaned posts behind
  }
}
