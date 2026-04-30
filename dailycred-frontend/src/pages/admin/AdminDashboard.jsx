import { useEffect, useState } from "react";
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

function ActionButton({ label, onClick, tone = "slate", disabled = false }) {
  const toneMap = {
    red: "bg-red-600 hover:bg-red-700 text-white",
    green: "bg-emerald-600 hover:bg-emerald-700 text-white",
    yellow: "bg-amber-500 hover:bg-amber-600 text-white",
    slate: "bg-slate-900 hover:bg-slate-800 text-white",
    light: "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50",
  };

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={`rounded-xl px-4 py-2 text-sm font-medium disabled:opacity-60 ${toneMap[tone] || toneMap.slate}`}
    >
      {label}
    </button>
  );
}

export default function AdminDashboard() {
  const { userId, displayName } = useAuth();

  const [adminInfo, setAdminInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState("");

  const [entityTargetType, setEntityTargetType] = useState("BORROWER");
  const [entityTargetId, setEntityTargetId] = useState("");
  const [reason, setReason] = useState("");

  const [walletOwnerType, setWalletOwnerType] = useState("BORROWER");
  const [walletOwnerId, setWalletOwnerId] = useState("");

  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");

  async function loadAdminInfo() {
    if (!userId) return;

    setLoading(true);
    setPageError("");

    try {
      const response = await api.get(`/api/admin/accounts/${userId}`);
      setAdminInfo(response.data?.data || null);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load admin account.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAdminInfo();
  }, [userId]);

  async function runEntityAction(actionType) {
    if (!entityTargetId) {
      setActionError("Please enter target ID.");
      setActionMessage("");
      return;
    }

    setActionLoading(true);
    setActionError("");
    setActionMessage("");

    try {
      const base =
        entityTargetType === "BORROWER"
          ? `/api/admin/oversight/borrowers/${entityTargetId}`
          : `/api/admin/oversight/lenders/${entityTargetId}`;

      let url = "";
      let body = undefined;

      switch (actionType) {
        case "freeze":
          url = `${base}/freeze`;
          body = { reason };
          break;
        case "unfreeze":
          url = `${base}/unfreeze`;
          break;
        case "fraud-flag":
          url = `${base}/fraud-flag`;
          body = { reason };
          break;
        case "clear-fraud-flag":
          url = `${base}/clear-fraud-flag`;
          break;
        case "blacklist":
          url = `${base}/blacklist`;
          body = { reason };
          break;
        case "remove-blacklist":
          url = `${base}/remove-blacklist`;
          break;
        default:
          throw new Error("Unsupported action");
      }

      const response = await api.post(url, body);
      setActionMessage(response.data?.message || "Action completed successfully.");
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || "Action failed.");
    } finally {
      setActionLoading(false);
    }
  }

  async function runWalletAction(actionType) {
    if (!walletOwnerId) {
      setActionError("Please enter wallet owner ID.");
      setActionMessage("");
      return;
    }

    setActionLoading(true);
    setActionError("");
    setActionMessage("");

    try {
      const url =
        actionType === "freeze"
          ? `/api/admin/oversight/wallets/${walletOwnerType}/${walletOwnerId}/freeze`
          : `/api/admin/oversight/wallets/${walletOwnerType}/${walletOwnerId}/unfreeze`;

      const response = await api.post(url);
      setActionMessage(response.data?.message || "Wallet action completed successfully.");
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || "Wallet action failed.");
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading admin dashboard...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">
          Welcome, {adminInfo?.adminName || displayName || "Admin"}
        </h2>
        <p className="mt-2 text-sm text-slate-500">
          Use this page to review admin account details and control borrower, lender, and wallet oversight actions.
        </p>

        {pageError ? (
          <div className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{pageError}</div>
        ) : null}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Admin Name"
          value={adminInfo?.adminName || "-"}
          subtitle="Current authenticated admin"
        />
        <SummaryCard
          title="Email"
          value={adminInfo?.email || "-"}
          subtitle="Admin login identifier"
        />
        <SummaryCard
          title="Super Admin"
          value={adminInfo?.superAdmin ? "YES" : "NO"}
          subtitle="Higher admin privilege marker"
        />
        <SummaryCard
          title="Notifications"
          value={adminInfo?.notificationEnabled ? "ON" : "OFF"}
          subtitle={adminInfo?.emailNotificationsEnabled ? "Email notifications ON" : "Email notifications OFF"}
        />
{/*          <a */}
{/*                 href="/notifications" */}
{/*                 className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*               > */}
{/*                 Open Notifications */}
{/*               </a> */}
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Borrower / Lender Oversight</h3>

          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Target Type</label>
              <select
                value={entityTargetType}
                onChange={(e) => setEntityTargetType(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
              >
                <option value="BORROWER">BORROWER</option>
                <option value="LENDER">LENDER</option>
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Target ID</label>
              <input
                value={entityTargetId}
                onChange={(e) => setEntityTargetId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Enter borrower or lender ID"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Reason</label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows="3"
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Reason for freeze, fraud flag, or blacklist"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <ActionButton label="Freeze" tone="yellow" disabled={actionLoading} onClick={() => runEntityAction("freeze")} />
              <ActionButton label="Unfreeze" tone="green" disabled={actionLoading} onClick={() => runEntityAction("unfreeze")} />
              <ActionButton label="Fraud Flag" tone="red" disabled={actionLoading} onClick={() => runEntityAction("fraud-flag")} />
              <ActionButton label="Clear Fraud" tone="light" disabled={actionLoading} onClick={() => runEntityAction("clear-fraud-flag")} />
              <ActionButton label="Blacklist" tone="red" disabled={actionLoading} onClick={() => runEntityAction("blacklist")} />
              <ActionButton label="Remove Blacklist" tone="light" disabled={actionLoading} onClick={() => runEntityAction("remove-blacklist")} />
            </div>
          </div>
        </div>

        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Wallet Oversight</h3>

          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Wallet Owner Type</label>
              <select
                value={walletOwnerType}
                onChange={(e) => setWalletOwnerType(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
              >
                <option value="BORROWER">BORROWER</option>
                <option value="LENDER">LENDER</option>
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Wallet Owner ID</label>
              <input
                value={walletOwnerId}
                onChange={(e) => setWalletOwnerId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Enter borrower or lender ID"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <ActionButton label="Freeze Wallet" tone="yellow" disabled={actionLoading} onClick={() => runWalletAction("freeze")} />
              <ActionButton label="Unfreeze Wallet" tone="green" disabled={actionLoading} onClick={() => runWalletAction("unfreeze")} />
            </div>
          </div>
        </div>
      </div>

      {(actionMessage || actionError) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-3 text-lg font-semibold text-slate-900">Action Result</h3>

          {actionMessage ? (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {actionMessage}
            </div>
          ) : null}

          {actionError ? (
            <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
              {actionError}
            </div>
          ) : null}
        </div>
      )}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">How this page works</h3>
        <div className="space-y-2 text-sm text-slate-600">
          <p>1. It loads the current admin account using the admin ID from your JWT-authenticated frontend session.</p>
          <p>2. It lets you choose borrower or lender as the admin action target.</p>
          <p>3. It sends the exact oversight endpoint calls already available in your backend.</p>
          <p>4. It also lets you freeze or unfreeze wallets using the admin wallet oversight APIs.</p>
        </div>
      </div>
    </div>
  );
}