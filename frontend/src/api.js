import axios from "axios";
import keycloak from "./keycloak";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true, // send the HttpOnly ACCESS_TOKEN cookie on every request
});

function readCookie(name) {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

// No Authorization header anymore - the cookie carries the access token.
// State-changing requests still need the CSRF header Spring expects,
// read from the (non-HttpOnly, by design) XSRF-TOKEN cookie it sets.
api.interceptors.request.use(async (config) => {
  if (["post", "put", "delete", "patch"].includes(config.method)) {
    const csrf = readCookie("XSRF-TOKEN");
    if (csrf) config.headers["X-XSRF-TOKEN"] = csrf;
  }
  return config;
});

/**
 * Call once right after keycloak.init() resolves, and again whenever the
 * token is refreshed: exchanges the in-memory Keycloak token for the
 * HttpOnly cookie the backend will use from then on.
 */
export async function syncSessionCookie() {
  await api.post("/auth/session", {
    accessToken: keycloak.token,
    expiresInSeconds: keycloak.tokenParsed?.exp
      ? keycloak.tokenParsed.exp - Math.floor(Date.now() / 1000)
      : 300,
  });
}

export const errorMessage = (err) =>
  err.response?.data?.message ||
  err.response?.data?.errors?.[0]?.defaultMessage ||
  (err.response?.status === 403 ? "You don't have permission to do that." : null) ||
  err.message ||
  "Something went wrong.";

export default api;
