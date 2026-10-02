package com.example.blog.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  /** Name of the HttpOnly cookie that carries the Keycloak access token. */
  public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    var csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
    // XSRF-TOKEN cookie is readable by JS on purpose (double-submit pattern);
    // the ACCESS_TOKEN cookie below is HttpOnly and never touched by JS.
    var csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();

    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfRepo)
            .csrfTokenRequestHandler(csrfTokenRequestHandler)
            // /api/auth/** issues/clears the cookie itself before a CSRF token exists yet
            .ignoringRequestMatchers("/api/auth/**", "/h2-console/**"))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
            .anyRequest().denyAll())
        .headers(h -> h.frameOptions(f -> f.sameOrigin())) // needed for the H2 console
        .oauth2ResourceServer(o -> o
            .bearerTokenResolver(cookieBearerTokenResolver())
            .jwt(j -> j.jwtAuthenticationConverter(jwtConverter())))
        // Spring defers writing the XSRF-TOKEN cookie until something reads
        // the token; for a pure JSON API nothing does that by default, so
        // force it on every request, or the frontend would never receive it.
        .addFilterAfter((request, response, chain) -> {
          var req = (jakarta.servlet.http.HttpServletRequest) request;
          Object attr = req.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
          if (attr instanceof java.util.function.Supplier<?> supplier) supplier.get();
          chain.doFilter(request, response);
        }, org.springframework.security.web.csrf.CsrfFilter.class);
    return http.build();
  }

  /**
   * Reads the access token from the HttpOnly ACCESS_TOKEN cookie instead of the
   * Authorization header, so the frontend never has to attach it manually.
   */
  @Bean
  BearerTokenResolver cookieBearerTokenResolver() {
    return request -> {
      if (request.getCookies() == null) return null;
      for (var c : request.getCookies()) {
        if (ACCESS_TOKEN_COOKIE.equals(c.getName())) return c.getValue();
      }
      return null;
    };
  }

  /** Turns Keycloak's realm_access.roles into Spring's ROLE_* authorities. */
  @SuppressWarnings("unchecked")
  private JwtAuthenticationConverter jwtConverter() {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      Map<String, Object> realmAccess = jwt.getClaim("realm_access");
      if (realmAccess == null || realmAccess.get("roles") == null) return List.of();
      return ((Collection<String>) realmAccess.get("roles")).stream()
          .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
          .collect(Collectors.toList());
    });
    return converter;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origin) {
    var cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(origin));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("X-XSRF-TOKEN"));
    cfg.setAllowCredentials(true); // required so the browser sends/receives our cookies
    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }
}
