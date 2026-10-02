import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../api";

const empty = { title: "", content: "" };

export default function BlogsPage() {
  const [scope, setScope] = useState("all"); // "all" | "me"
  const [blogs, setBlogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await api.get(scope === "me" ? "/blogs/me" : "/blogs");
      setBlogs(data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [scope]);

  useEffect(() => { load(); }, [load]);

  const reset = () => { setForm(empty); setEditingId(null); };

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (editingId) await api.put(`/blogs/${editingId}`, form);
      else await api.post("/blogs", form);
      reset();
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const edit = (b) => {
    setEditingId(b.id);
    setForm({ title: b.title, content: b.content });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async (b) => {
    if (!window.confirm(`Delete "${b.title}"?`)) return;
    try {
      await api.delete(`/blogs/${b.id}`);
      if (editingId === b.id) reset();
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <>
      <section className="composer">
        <h2>{editingId ? "Edit post" : "Write a post"}</h2>
        <form onSubmit={submit}>
          <label>
            Title
            <input value={form.title} maxLength={200} required
                   onChange={(e) => setForm({ ...form, title: e.target.value })} />
          </label>
          <label>
            Content
            <textarea rows={6} value={form.content} required
                      onChange={(e) => setForm({ ...form, content: e.target.value })} />
          </label>
          <div className="row">
            <button className="primary" disabled={saving}>
              {saving ? "Saving…" : editingId ? "Save changes" : "Publish"}
            </button>
            {editingId && <button type="button" className="ghost" onClick={reset}>Cancel</button>}
          </div>
        </form>
      </section>

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={scope === "all"} onClick={() => setScope("all")}>All blogs</button>
        <button role="tab" aria-selected={scope === "me"} onClick={() => setScope("me")}>My blogs</button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {loading && <p className="muted">Loading…</p>}
      {!loading && blogs.length === 0 && (
        <p className="muted">
          {scope === "me" ? "You haven't published anything yet. Write your first post above." : "No posts yet."}
        </p>
      )}

      {blogs.map((b) => (
        <article className="post" key={b.id}>
          <h3>{b.title}</h3>
          <p className="meta">
            {b.authorName} on {new Date(b.createdAt).toLocaleDateString()}
            {b.updatedAt && " (edited)"}
          </p>
          <p className="body">{b.content}</p>
          {b.canModify && (
            <div className="row">
              <button className="link" onClick={() => edit(b)}>Edit</button>
              <button className="link danger" onClick={() => remove(b)}>Delete</button>
            </div>
          )}
        </article>
      ))}
    </>
  );
}
