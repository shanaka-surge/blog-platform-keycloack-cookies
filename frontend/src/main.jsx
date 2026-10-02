import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import keycloak from "./keycloak";
import { syncSessionCookie } from "./api";
import App from "./App";
import LoginPage from "./pages/LoginPage";
import "./styles.css";

// No <React.StrictMode>: its double-invoked effects break keycloak.init() in dev.
keycloak
  .init({
    onLoad: "check-sso", // render our own page instead of auto-redirecting
    pkceMethod: "S256",
    checkLoginIframe: false,
    silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
  })
  .then(async (authenticated) => {
    const root = ReactDOM.createRoot(document.getElementById("root"));
    if (authenticated) {
      // Hand the token to the backend once; from here on the HttpOnly
      // ACCESS_TOKEN cookie carries it. keycloak.token is only kept around
      // in memory so it can be refreshed and re-synced below.
      await syncSessionCookie();

      // Re-sync the cookie whenever the token is refreshed, instead of
      // attaching keycloak.token to each request by hand like before.
      keycloak.onTokenExpired = () => {
        keycloak.updateToken(30).then(syncSessionCookie).catch(() => keycloak.login());
      };
      setInterval(() => keycloak.updateToken(30).then(syncSessionCookie).catch(() => {}), 20000);

      root.render(
        <BrowserRouter>
          <App />
        </BrowserRouter>
      );
    } else {
      root.render(<LoginPage />);
    }
  })
  .catch((e) => {
    document.getElementById("root").textContent =
      "Could not reach the login server. Is Keycloak running on " + import.meta.env.VITE_KEYCLOAK_URL + "?";
    console.error(e);
  });
