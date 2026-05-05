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

export default function BorrowerDashboard() {
  const { userId, displayName } = useAuth();

  const [summary, setSummary] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [repayments, setRepayments] = useState([]);
  const [notifications, setNotifications] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const unreadCount = useMemo(() => {
    return notifications.filter((item) => !item.readFlag).length;
  }, [notifications]);

  async function loadDashboard() {
    if (!userId) return;

    setLoading(true);
    setError("");

    try {
      const results = await Promise.allSettled([
        api.get(`/api/borrower-analytics/${userId}/summary`),
        api.get(`/api/wallets/BORROWER/${userId}`),
        api.get(`/api/repayments/borrower/${userId}`),
        api.get(`/api/notifications/BORROWER/${userId}`),
      ]);

      const summaryResult = results[0];
      const walletResult = results[1];
      const repaymentsResult = results[2];
      const notificationsResult = results[3];

      if (summaryResult.status === "fulfilled") {
        setSummary(summaryResult.value.data?.data || null);
      }

      if (walletResult.status === "fulfilled") {
        setWallet(walletResult.value.data || null);
      }

      if (repaymentsResult.status === "fulfilled") {
        setRepayments(repaymentsResult.value.data || []);
      }

      if (notificationsResult.status === "fulfilled") {
        setNotifications(notificationsResult.value.data || []);
      }

      const summaryFailed = summaryResult.status === "rejected";
      const walletFailed = walletResult.status === "rejected";
      const repaymentsFailed = repaymentsResult.status === "rejected";
      const notificationsFailed = notificationsResult.status === "rejected";

      if (summaryFailed && walletFailed && repaymentsFailed && notificationsFailed) {
        throw new Error("Failed to load borrower dashboard data.");
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Failed to load dashboard.");
    } finally {
      setLoading(false);
    }
  }

  async function markNotificationAsRead(notificationId) {
    try {
      await api.post(`/api/notifications/${notificationId}/read`);
      setNotifications((prev) =>
        prev.map((item) =>
          item.notificationId === notificationId
            ? { ...item, readFlag: true, status: "READ" }
            : item
        )
      );
    } catch (err) {
      console.error("Failed to mark notification as read", err);
    }
  }

  useEffect(() => {
    loadDashboard();
  }, [userId]);

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading borrower dashboard...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">
          Welcome, {summary?.borrowerName || displayName || "Borrower"}
        </h2>
        <p className="mt-2 text-sm text-slate-500">
          This page shows your loan risk profile, repayment activity, wallet balance, and notifications.
        </p>

{/*         <div className="flex flex-wrap gap-3"> */}
{/*           <a */}
{/*             href="/kyc" */}
{/*             className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*           > */}
{/*             Open KYC Center */}
{/*           </a> */}
{/*           <a */}
{/*             href="/wallet" */}
{/*             className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*           > */}
{/*             Open Wallet */}
{/*           </a> */}
{/*           <a */}
{/*             href="/location" */}
{/*             className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*           > */}
{/*             Open Location Center */}
{/*           </a> */}
{/*           <a */}
{/*             href="/reports" */}
{/*             className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*           > */}
{/*             Open Reports Center */}
{/*           </a> */}
{/*           <a */}
{/*             href="/notifications" */}
{/*             className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*           > */}
{/*             Open Notifications */}
{/*           </a> */}
{/*           <a */}
{/*             href="/borrower/loan-apply" */}
{/*             className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*           > */}
{/*             Apply For Loan */}
{/*           </a> */}
{/*         </div> */}

        {error ? (
          <div className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
        ) : null}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Internal Credit Score"
          value={summary?.internalCreditScore ?? "-"}
          subtitle="Generated by your platform profile + behavior"
        />
        <SummaryCard
          title="Risk Score"
          value={summary?.riskScore ?? "-"}
          subtitle={summary?.riskCategory || "Not assessed"}
        />
        <SummaryCard
          title="Eligibility"
          value={summary?.eligibilityTier || "-"}
          subtitle={summary?.eligibilityStatus || "No status"}
        />
        <SummaryCard
          title="Current Outstanding"
          value={summary?.currentOutstandingAmount ?? 0}
          subtitle="Remaining borrower-side obligation"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="rounded-2xl bg-white p-6 shadow-sm xl:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-slate-900">Borrower Analytics Summary</h3>
            <button
              onClick={loadDashboard}
              className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Refresh
            </button>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Risk Category</p>
              <div className="mt-2">
                <StatusPill
                  text={summary?.riskCategory || "UNASSESSED"}
                  tone={
                    summary?.riskCategory === "LOW"
                      ? "green"
                      : summary?.riskCategory === "MEDIUM"
                      ? "yellow"
                      : summary?.riskCategory === "HIGH"
                      ? "red"
                      : "slate"
                  }
                />
              </div>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Wallet Balance</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">
                {wallet?.balance ?? 0}
              </h4>
              <p className="mt-1 text-xs text-slate-500">
                {wallet?.frozen ? "Wallet is frozen" : "Wallet is active"}
              </p>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Total Loans Taken</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">
                {summary?.totalLoansTaken ?? 0}
              </h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Active Loans</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">
                {summary?.activeLoans ?? 0}
              </h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Total Missed Days</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">
                {summary?.totalMissedDays ?? 0}
              </h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Late Payments</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">
                {summary?.totalLatePayments ?? 0}
              </h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Advance Payments</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">
                {summary?.totalAdvancePayments ?? 0}
              </h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Max Eligible Loan Amount</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">
                {summary?.maxEligibleLoanAmount ?? 0}
              </h4>
            </div>
          </div>
        </div>

        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-slate-900">Notifications</h3>
            <StatusPill text={`${unreadCount} unread`} tone={unreadCount > 0 ? "blue" : "slate"} />
          </div>

          <div className="space-y-3">
            {notifications.length === 0 ? (
              <p className="text-sm text-slate-500">
                No notifications yet. Later we will wire automatic notifications for loan approvals, repayments, flags, blacklist events, and scheduler alerts.
              </p>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.notificationId}
                  className={`rounded-2xl border p-4 ${
                    item.readFlag ? "border-slate-200 bg-slate-50" : "border-blue-200 bg-blue-50"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h4 className="font-semibold text-slate-900">{item.title}</h4>
                      <p className="mt-1 text-sm text-slate-600">{item.message}</p>
                      <p className="mt-2 text-xs text-slate-500">
                        {item.createdAt ? new Date(item.createdAt).toLocaleString() : ""}
                      </p>
                    </div>

                    {!item.readFlag ? (
                      <button
                        onClick={() => markNotificationAsRead(item.notificationId)}
                        className="rounded-xl border border-slate-300 px-3 py-2 text-xs font-medium text-slate-700"
                      >
                        Mark Read
                      </button>
                    ) : (
                      <StatusPill text="Read" tone="green" />
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Repayment History</h3>
          <p className="text-sm text-slate-500">Fetched from your borrower repayment endpoint</p>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Repayment ID</th>
                <th className="px-3 py-3">Date</th>
                <th className="px-3 py-3">Amount</th>
                <th className="px-3 py-3">Mode</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Missed Days</th>
                <th className="px-3 py-3">Balance</th>
              </tr>
            </thead>
            <tbody>
              {repayments.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-3 py-6 text-center text-slate-500">
                    No repayments found yet.
                  </td>
                </tr>
              ) : (
                repayments.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.id}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentDate}</td>
                    <td className="px-3 py-3 text-slate-700">{item.amountPaid}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentMode}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentStatus}</td>
                    <td className="px-3 py-3 text-slate-700">{item.missedDays}</td>
                    <td className="px-3 py-3 text-slate-700">{item.balanceAmount}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}