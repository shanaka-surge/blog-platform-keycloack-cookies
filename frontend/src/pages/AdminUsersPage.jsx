import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../api";

const blank = { username: "", email: "", password: "", admin: false };

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(blank);
  const [editing, setEditing] = useState(null); // user being edited
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get("/admin/users");
      setUsers(data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const startEdit = (u) => {
    setEditing(u);
    setForm({ username: u.username, email: u.email || "", password: "", admin: false, enabled: u.enabled });
  };
  const reset = () => { setEditing(null); setForm(blank); };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (editing) {
        await api.put(`/admin/users/${editing.id}`, {
          email: form.email, enabled: form.enabled, password: form.password || null,
        });
      } else {
        await api.post("/admin/users", form);
      }
      reset();
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (u) => {
    if (!window.confirm(`Delete ${u.username}? Their blogs will be deleted too.`)) return;
    try {
      await api.delete(`/admin/users/${u.id}`);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <>
      <section className="composer">
        <h2>{editing ? `Edit ${editing.username}` : "Add a user"}</h2>
        <form onSubmit={submit}>
          <div className="grid">
            <label>
              Username
              <input value={form.username} required disabled={!!editing}
                     onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </label>
            <label>
              Email
              <input type="email" value={form.email} required
                     onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
          </div>
          <label>
            {editing ? "New password (leave blank to keep the current one)" : "Password"}
            <input type="password" value={form.password} minLength={editing ? 0 : 6} required={!editing}
                   onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </label>
          {editing ? (
            <label className="check">
              <input type="checkbox" checked={form.enabled}
                     onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
              Account enabled
            </label>
          ) : (
            <label className="check">
              <input type="checkbox" checked={form.admin}
                     onChange={(e) => setForm({ ...form, admin: e.target.checked })} />
              Make this user an admin
            </label>
          )}
          <div className="row">
            <button className="primary" disabled={saving}>
              {saving ? "Saving…" : editing ? "Save changes" : "Add user"}
            </button>
            {editing && <button type="button" className="ghost" onClick={reset}>Cancel</button>}
          </div>
        </form>
      </section>

      {error && <p className="error" role="alert">{error}</p>}
      {loading ? <p className="muted">Loading…</p> : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Username</th><th>Email</th><th>Status</th><th><span className="sr">Actions</span></th></tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td>{u.enabled ? "Active" : "Disabled"}</td>
                  <td className="actions">
                    <button className="link" onClick={() => startEdit(u)}>Edit</button>
                    <button className="link danger" onClick={() => remove(u)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
