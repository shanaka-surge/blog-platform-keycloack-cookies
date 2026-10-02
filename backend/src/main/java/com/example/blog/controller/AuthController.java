package com.example.blog.controller;

import com.example.blog.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Turns a Keycloak access token (obtained by the frontend via the normal OIDC
 * redirect flow) into an HttpOnly cookie, so the frontend never has to store
 * it in memory/localStorage or attach it to requests by hand.
 *
 * The token itself passes through browser JS exactly once, on the call to
 * POST /api/auth/session right after login - the same moment keycloak-js
 * would otherwise have put it in memory. After that this cookie is the only
 * copy the browser holds, and JS can no longer read it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Value("${app.cookie-secure:false}")
  private boolean cookieSecure; // set true once served over HTTPS

  public record SessionRequest(@NotBlank String accessToken, Integer expiresInSeconds) {}

  @PostMapping("/session")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void startSession(@RequestBody SessionRequest req, HttpServletResponse response) {
    Cookie cookie = new Cookie(SecurityConfig.ACCESS_TOKEN_COOKIE, req.accessToken());
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge(req.expiresInSeconds() != null ? req.expiresInSeconds() : 300);
    // "Lax" lets the cookie ride along on normal navigation/XHR from your own
    // frontend origin, while still blocking it on cross-site POSTs - the main
    // CSRF defense, on top of the X-XSRF-TOKEN header check Spring adds.
    response.addHeader("Set-Cookie", cookie.getName() + "=" + cookie.getValue()
        + "; Path=/; Max-Age=" + cookie.getMaxAge()
        + "; HttpOnly" + (cookieSecure ? "; Secure" : "") + "; SameSite=Lax");
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void endSession(HttpServletResponse response) {
    response.addHeader("Set-Cookie", SecurityConfig.ACCESS_TOKEN_COOKIE
        + "=; Path=/; Max-Age=0; HttpOnly" + (cookieSecure ? "; Secure" : "") + "; SameSite=Lax");
  }
}
