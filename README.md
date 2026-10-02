# Blog platform: React + Spring Boot + H2 + Keycloak

## Layout
- `keycloak/realm-export.json`: realm, roles, clients and test users, imported automatically
- `docker-compose.yml`: runs Keycloak on :8080
- `backend/`: Spring Boot API on :8081
- `frontend/`: React (Vite) on :5173

## Run it (3 terminals)
1. Keycloak: `docker compose up`  (wait for "Running the server in development mode")
2. Backend:  `cd backend && mvn spring-boot:run`   (Java 17+)
3. Frontend: `cd frontend && npm install && npm run dev`

Open http://localhost:5173.

## Test accounts (realm `blog-realm`)
| user  | password | roles       |
|-------|----------|-------------|
| alice | password | USER        |
| boss  | password | USER, ADMIN |

Keycloak's own admin console: http://localhost:8080 (admin / admin).
Anyone can also register from the login page and gets USER automatically.
H2 console: http://localhost:8081/h2-console  (JDBC URL `jdbc:h2:file:./data/blogdb`, user `sa`, empty password).

## Auth model: cookie-based (not headers)

The frontend no longer attaches `Authorization: Bearer <token>` to requests. Instead:

1. React still logs in via Keycloak's redirect flow (`keycloak-js`), same as before.
2. Right after login, and after every token refresh, React POSTs the access token once to
   `POST /api/auth/session`. The backend stores it as an `HttpOnly` cookie (`ACCESS_TOKEN`) -
   from then on the browser attaches it automatically; JS can no longer read it.
3. Spring Security reads the token from that cookie (`cookieBearerTokenResolver`) instead of
   a header.
4. Because cookies are vulnerable to CSRF (headers aren't), CSRF protection is back on for
   everything except `/api/auth/**`. Spring issues a non-HttpOnly `XSRF-TOKEN` cookie; the
   frontend reads it and echoes it back as an `X-XSRF-TOKEN` header on every POST/PUT/DELETE
   (`frontend/src/api.js`).
5. `POST /api/auth/logout` clears the cookie server-side before the normal Keycloak logout
   redirect runs.

This trades "token briefly touches JS memory once per login/refresh" for "token can never be
read by injected JS afterward, at the cost of needing CSRF protection." See the chat history
in this project for the fuller trade-off discussion if you're deciding whether to keep this.

## API
| Method | Path | Who |
|---|---|---|
| GET | /api/blogs | any logged-in user: all blogs |
| GET | /api/blogs/me | any logged-in user: own blogs |
| POST | /api/blogs | any logged-in user |
| PUT / DELETE | /api/blogs/{id} | author only (set `app.admin-can-modify-blogs: true` to let admins too) |
| GET/POST/PUT/DELETE | /api/admin/users[/{id}] | ADMIN only, proxied to Keycloak |

## Before production
Change the `blog-admin-service` secret, the Keycloak admin password, turn off `directAccessGrantsEnabled` on `blog-frontend`, use HTTPS, and run Keycloak with a real database.
