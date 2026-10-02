import { NavLink } from "react-router-dom";
import keycloak from "../keycloak";
import api from "../api";

export default function Navbar({ isAdmin }) {
  const name = keycloak.tokenParsed?.preferred_username;

  const logout = async () => {
    try {
      await api.post("/auth/logout"); // clear the HttpOnly cookie server-side first
    } catch {
      // even if this fails, still send the user through the real Keycloak logout below
    }
    keycloak.logout({ redirectUri: window.location.origin });
  };

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <span className="brand">Marginalia</span>
        <nav>
          <NavLink to="/" end>Blogs</NavLink>
          {isAdmin && <NavLink to="/admin/users">Users</NavLink>}
        </nav>
        <div className="who">
          <span>{name}{isAdmin && <em className="badge">admin</em>}</span>
          <button className="link" onClick={logout}>Log out</button>
        </div>
      </div>
    </header>
  );
}
