import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import api from "../api/client";
import { useAuth } from "../auth/AuthContext";

export default function AppShell() {
  const navigate = useNavigate();
  const { role, userId, displayName, logout } = useAuth();

  const [unreadCount, setUnreadCount] = useState(0);

  const targetType = useMemo(() => {
    if (role === "BORROWER") return "BORROWER";
    if (role === "LENDER") return "LENDER";
    if (role === "ADMIN") return "ADMIN";
    return null;
  }, [role]);

  const homePath = useMemo(() => {
    if (role === "BORROWER") return "/borrower/dashboard";
    if (role === "LENDER") return "/lender/dashboard";
    if (role === "ADMIN") return "/admin/dashboard";
    return "/login";
  }, [role]);

  const navItems = useMemo(() => {
    if (role === "BORROWER") {
      return [
        { label: "Dashboard", path: "/borrower/dashboard" },
        { label: "Discover & Apply", path: "/borrower/loan-apply" },
        { label: "Repay Loans", path: "/borrower/repayments" },
        { label: "Wallet", path: "/wallet" },
        { label: "KYC & OCR", path: "/kyc" },
        { label: "Location", path: "/location" },
        { label: "Reports", path: "/reports" },
        { label: "Notifications", path: "/notifications" },
      ];
    }

    if (role === "LENDER") {
      return [
        { label: "Dashboard", path: "/lender/dashboard" },
        { label: "Loan Plans", path: "/lender/loan-plans" },
        { label: "Review Applications", path: "/lender/review-applications" },
        { label: "Repayment Tracking", path: "/lender/repayments" },
        { label: "Wallet", path: "/wallet" },
        { label: "KYC & OCR", path: "/kyc" },
        { label: "Location", path: "/location" },
        { label: "Reports", path: "/reports" },
        { label: "Notifications", path: "/notifications" },
      ];
    }

    if (role === "ADMIN") {
      return [
        { label: "Dashboard", path: "/admin/dashboard" },
        { label: "Notifications", path: "/notifications" },
      ];
    }

    return [];
  }, [role]);

  async function loadUnreadCount() {
    if (!targetType || !userId) return;

    try {
      const response = await api.get(`/api/notifications/${targetType}/${userId}`);
      const list = response.data || [];
      const unread = list.filter((item) => !item.readFlag).length;
      setUnreadCount(unread);
    } catch (error) {
      setUnreadCount(0);
    }
  }

  useEffect(() => {
    loadUnreadCount();

    const intervalId = setInterval(() => {
      loadUnreadCount();
    }, 15000);

    return () => clearInterval(intervalId);
  }, [targetType, userId]);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  function navLinkClass({ isActive }) {
    return [
      "block rounded-xl px-4 py-3 text-sm font-medium transition",
      isActive
        ? "bg-slate-900 text-white"
        : "text-slate-700 hover:bg-slate-100",
    ].join(" ");
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-40 border-b bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">DailyCred</h1>
            <p className="text-sm text-slate-500">Role: {role || "Guest"}</p>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              title="Go Back"
            >
              ← Back
            </button>

            <button
              type="button"
              onClick={() => navigate(homePath)}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              title="Go Home"
            >
              ⌂ Home
            </button>

            <button
              type="button"
              onClick={() => navigate("/notifications")}
              className="relative rounded-xl border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              title="Notifications"
            >
              🔔
              {unreadCount > 0 ? (
                <span className="absolute -right-1 -top-1 inline-flex min-w-[20px] items-center justify-center rounded-full bg-red-600 px-1.5 py-0.5 text-[10px] font-bold text-white">
                  {unreadCount > 99 ? "99+" : unreadCount}
                </span>
              ) : null}
            </button>

            <div className="hidden text-sm text-slate-600 md:block">
              {displayName || "User"}
            </div>

            <button
              type="button"
              onClick={handleLogout}
              className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto grid max-w-7xl grid-cols-1 gap-6 px-6 py-6 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside className="rounded-2xl bg-white p-4 shadow-sm">
          <div className="mb-4 border-b pb-4">
            <p className="text-sm font-medium text-slate-500">Navigation</p>
            <p className="mt-1 text-base font-semibold text-slate-900">{displayName || "User"}</p>
          </div>

          <nav className="space-y-2">
            {navItems.map((item) => (
              <NavLink key={item.path} to={item.path} className={navLinkClass}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}