import { useEffect, useMemo, useState } from "react";
import api from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

function SummaryCard({ title, value, subtitle }) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
      <p className="text-sm font-medium text-slate-500">{title}</p>
      <h3 className="mt-2 text-2xl font-bold text-slate-900">{value}</h3>
      {subtitle ? <p className="mt-1 text-xs text-slate-500">{subtitle}</p> : null}
    </div>
  );
}

function StatusPill({ text, tone = "slate" }) {
  const toneMap = {
    green: "bg-emerald-50 text-emerald-700",
    red: "bg-red-50 text-red-700",
    yellow: "bg-amber-50 text-amber-700",
    blue: "bg-blue-50 text-blue-700",
    slate: "bg-slate-100 text-slate-700",
  };

  return (
    <span className={`rounded-full px-3 py-1 text-xs font-medium ${toneMap[tone] || toneMap.slate}`}>
      {text}
    </span>
  );
}

export default function NotificationsPage() {
  const { role, userId } = useAuth();

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [pageError, setPageError] = useState("");
  const [pageMessage, setPageMessage] = useState("");
  const [filter, setFilter] = useState("ALL");

  const targetType = useMemo(() => {
    if (role === "BORROWER") return "BORROWER";
    if (role === "LENDER") return "LENDER";
    if (role === "ADMIN") return "ADMIN";
    return null;
  }, [role]);

  const unreadCount = useMemo(() => {
    return notifications.filter((item) => !item.readFlag).length;
  }, [notifications]);

  const filteredNotifications = useMemo(() => {
    if (filter === "UNREAD") {
      return notifications.filter((item) => !item.readFlag);
    }
    return notifications;
  }, [notifications, filter]);

  async function loadNotifications(showMessage = false) {
    if (!targetType || !userId) return;

    if (notifications.length === 0) {
      setLoading(true);
    }

    setPageError("");

    try {
      const response = await api.get(`/api/notifications/${targetType}/${userId}`);
      const data = response.data || [];
      setNotifications(data);

      if (showMessage) {
        setPageMessage("Notifications refreshed successfully.");
      }
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load notifications.");
    } finally {
      setLoading(false);
    }
  }

  async function markAsRead(notificationId) {
    setActionLoadingId(notificationId);
    setPageError("");
    setPageMessage("");

    try {
      await api.post(`/api/notifications/${notificationId}/read`);

      setNotifications((prev) =>
        prev.map((item) =>
          item.notificationId === notificationId
            ? {
                ...item,
                readFlag: true,
                status: "READ",
                readAt: new Date().toISOString(),
              }
            : item
        )
      );

      setPageMessage("Notification marked as read.");
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to mark notification as read.");
    } finally {
      setActionLoadingId(null);
    }
  }

  useEffect(() => {
    loadNotifications(false);

    const intervalId = setInterval(() => {
      loadNotifications(false);
    }, 15000);

    return () => clearInterval(intervalId);
  }, [targetType, userId]);

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading notifications...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Notifications Center</h2>
        <p className="mt-2 text-sm text-slate-500">
          These are your in-app notifications. This page refreshes automatically every 15 seconds.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard
          title="Target Type"
          value={targetType || "-"}
          subtitle="Derived from your logged-in role"
        />
        <SummaryCard
          title="Total Notifications"
          value={notifications.length}
          subtitle="All notifications currently loaded"
        />
        <SummaryCard
          title="Unread Notifications"
          value={unreadCount}
          subtitle="Unread count based on readFlag"
        />
      </div>

      {(pageMessage || pageError) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          {pageMessage ? (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {pageMessage}
            </div>
          ) : null}

          {pageError ? (
            <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
              {pageError}
            </div>
          ) : null}
        </div>
      )}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-lg font-semibold text-slate-900">Your Notifications</h3>

          <div className="flex flex-wrap items-center gap-3">
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="ALL">Show All</option>
              <option value="UNREAD">Show Unread Only</option>
            </select>

            <button
              onClick={() => loadNotifications(true)}
              className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Refresh Now
            </button>
          </div>
        </div>

        <div className="space-y-4">
          {filteredNotifications.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500">
              No notifications found for the selected filter.
            </div>
          ) : (
            filteredNotifications.map((item) => (
              <div
                key={item.notificationId}
                className={`rounded-2xl border p-5 ${
                  item.readFlag ? "border-slate-200 bg-slate-50" : "border-blue-200 bg-blue-50"
                }`}
              >
                <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusPill
                      text={item.readFlag ? "READ" : "UNREAD"}
                      tone={item.readFlag ? "green" : "blue"}
                    />
                    <StatusPill text={item.channelType || "IN_APP"} tone="slate" />
                    <StatusPill text={item.status || "CREATED"} tone="yellow" />
                  </div>

                  {!item.readFlag ? (
                    <button
                      onClick={() => markAsRead(item.notificationId)}
                      disabled={actionLoadingId === item.notificationId}
                      className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
                    >
                      {actionLoadingId === item.notificationId ? "Marking..." : "Mark as Read"}
                    </button>
                  ) : null}
                </div>

                <h4 className="text-lg font-semibold text-slate-900">{item.title}</h4>
                <p className="mt-2 text-sm leading-6 text-slate-700">{item.message}</p>

                <div className="mt-4 grid grid-cols-1 gap-3 text-xs text-slate-500 md:grid-cols-3">
                  <div>
                    <p className="font-medium text-slate-600">Created At</p>
                    <p>{item.createdAt ? new Date(item.createdAt).toLocaleString() : "-"}</p>
                  </div>

                  <div>
                    <p className="font-medium text-slate-600">Sent At</p>
                    <p>{item.sentAt ? new Date(item.sentAt).toLocaleString() : "-"}</p>
                  </div>

                  <div>
                    <p className="font-medium text-slate-600">Read At</p>
                    <p>{item.readAt ? new Date(item.readAt).toLocaleString() : "-"}</p>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-3 text-lg font-semibold text-slate-900">How this page works</h3>
        <div className="space-y-2 text-sm text-slate-600">
          <p>1. The page maps your logged-in role to a notification target type: BORROWER, LENDER, or ADMIN.</p>
          <p>2. It loads notifications from the backend using your target type and user ID.</p>
          <p>3. It refreshes every 15 seconds because we are not using WebSockets yet.</p>
          <p>4. When backend starts creating notifications on events like repayment, flagging, blacklist, freeze, or approval, this page will immediately start showing them.</p>
        </div>
      </div>
    </div>
  );
}