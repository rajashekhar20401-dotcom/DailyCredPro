import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({
    identifier: "",
    password: "",
    role: "BORROWER",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await api.post("/api/auth/login", form);
      const payload = response.data?.data;

      if (!payload?.token) {
        throw new Error(response.data?.message || "Login failed");
      }

      login(payload);

      if (payload.role === "BORROWER") navigate("/borrower/dashboard");
      else if (payload.role === "LENDER") navigate("/lender/dashboard");
      else navigate("/admin/dashboard");
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
      <div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-lg">
        <h1 className="mb-2 text-3xl font-bold text-slate-900">DailyCred Login</h1>
        <p className="mb-6 text-sm text-slate-500">
          Login as borrower, lender, or admin.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Role</label>
            <select
              name="role"
              value={form.role}
              onChange={handleChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
            >
              <option value="BORROWER">Borrower</option>
              <option value="LENDER">Lender</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">
              {form.role === "ADMIN" ? "Email" : "Phone Number"}
            </label>
            <input
              name="identifier"
              value={form.identifier}
              onChange={handleChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
              placeholder={form.role === "ADMIN" ? "admin@example.com" : "9000000001"}
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Password</label>
            <div className="flex gap-2">
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                value={form.password}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Enter password"
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="rounded-xl border border-slate-300 px-4 py-2 text-sm"
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
          </div>

          {error ? (
            <div className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
          ) : null}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-slate-900 px-4 py-3 font-medium text-white disabled:opacity-60"
          >
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>

        <div className="mt-6 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <Link
            to="/borrower/register"
            className="rounded-xl border border-slate-300 px-4 py-3 text-center font-medium text-slate-700 hover:bg-slate-50"
          >
            Register as Borrower
          </Link>

          <Link
            to="/lender/register"
            className="rounded-xl border border-slate-300 px-4 py-3 text-center font-medium text-slate-700 hover:bg-slate-50"
          >
            Register as Lender
          </Link>
        </div>
      </div>
    </div>
  );
}