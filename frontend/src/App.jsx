import { Navigate, Route, Routes } from "react-router-dom";
import keycloak from "./keycloak";
import Navbar from "./components/Navbar";
import BlogsPage from "./pages/BlogsPage";
import AdminUsersPage from "./pages/AdminUsersPage";

export default function App() {
  const isAdmin = keycloak.hasRealmRole("ADMIN");
  return (
    <>
      <Navbar isAdmin={isAdmin} />
      <main>
        <Routes>
          <Route path="/" element={<BlogsPage />} />
          {/* UI guard only; the API enforces ADMIN on its own. */}
          <Route path="/admin/users" element={isAdmin ? <AdminUsersPage /> : <Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
