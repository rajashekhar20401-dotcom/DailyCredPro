import { useEffect, useMemo, useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  CartesianGrid,
  Legend,
} from "recharts";
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

export default function LenderDashboard() {
  const { userId, displayName } = useAuth();

  const [summary, setSummary] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [todayCollections, setTodayCollections] = useState([]);
  const [planPerformance, setPlanPerformance] = useState([]);
  const [borrowerRiskBreakdown, setBorrowerRiskBreakdown] = useState([]);
  const [notifications, setNotifications] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const unreadCount = useMemo(() => {
    return notifications.filter((item) => !item.readFlag).length;
  }, [notifications]);

  const repaymentStatusChartData = useMemo(() => {
    if (!summary) return [];
    return [
      { name: "Paid Today", value: summary.paidToday || 0 },
      { name: "Missed Today", value: summary.missedToday || 0 },
      { name: "Partial Today", value: summary.partialToday || 0 },
      { name: "Advance Today", value: summary.advanceToday || 0 },
    ];
  }, [summary]);

  const planChartData = useMemo(() => {
    return planPerformance.map((item) => ({
      name: item.planName,
      disbursed: item.totalDisbursed || 0,
      collected: item.totalCollected || 0,
      outstanding: item.totalOutstanding || 0,
      projectedProfit: item.projectedProfit || 0,
    }));
  }, [planPerformance]);

  const borrowerRiskChartData = useMemo(() => {
    return borrowerRiskBreakdown.map((item) => ({
      name: item.borrowerName,
      riskScore: item.riskScore || 0,
      outstanding: item.currentOutstanding || 0,
    }));
  }, [borrowerRiskBreakdown]);

  async function loadDashboard() {
    if (!userId) return;

    setLoading(true);
    setError("");

    try {
      const results = await Promise.allSettled([
        api.get(`/api/lender-analytics/${userId}/dashboard`),
        api.get(`/api/lender-analytics/${userId}/today-collections`),
        api.get(`/api/lender-analytics/${userId}/loan-plan-performance`),
        api.get(`/api/lender-analytics/${userId}/borrower-risk-breakdown`),
        api.get(`/api/wallets/LENDER/${userId}`),
        api.get(`/api/notifications/LENDER/${userId}`),
      ]);

      const summaryResult = results[0];
      const todayCollectionsResult = results[1];
      const planPerformanceResult = results[2];
      const borrowerRiskResult = results[3];
      const walletResult = results[4];
      const notificationsResult = results[5];

      if (summaryResult.status === "fulfilled") {
        setSummary(summaryResult.value.data || null);
      }

      if (todayCollectionsResult.status === "fulfilled") {
        setTodayCollections(todayCollectionsResult.value.data || []);
      }

      if (planPerformanceResult.status === "fulfilled") {
        setPlanPerformance(planPerformanceResult.value.data || []);
      }

      if (borrowerRiskResult.status === "fulfilled") {
        setBorrowerRiskBreakdown(borrowerRiskResult.value.data || []);
      }

      if (walletResult.status === "fulfilled") {
        setWallet(walletResult.value.data || null);
      }

      if (notificationsResult.status === "fulfilled") {
        setNotifications(notificationsResult.value.data || []);
      }

      const allFailed =
        summaryResult.status === "rejected" &&
        todayCollectionsResult.status === "rejected" &&
        planPerformanceResult.status === "rejected" &&
        borrowerRiskResult.status === "rejected" &&
        walletResult.status === "rejected" &&
        notificationsResult.status === "rejected";

      if (allFailed) {
        throw new Error("Failed to load lender dashboard data.");
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
        <p className="text-slate-600">Loading lender dashboard...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">
          Welcome, {displayName || "Lender"}
        </h2>
        <p className="mt-2 text-sm text-slate-500">
          This page shows lender analytics, collections, borrower risk, wallet balance, and notifications.
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
{/*             href="/lender/loan-plans" */}
{/*             className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50" */}
{/*           > */}
{/*             Manage Loan Plans */}
{/*           </a> */}
{/*         </div> */}

        {error ? (
          <div className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
        ) : null}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Total Principal Disbursed"
          value={summary?.totalPrincipalDisbursed ?? 0}
          subtitle="Total amount released to borrowers"
        />
        <SummaryCard
          title="Total Collected"
          value={summary?.totalCollected ?? 0}
          subtitle="All repayments received"
        />
        <SummaryCard
          title="Projected Profit"
          value={summary?.projectedProfit ?? 0}
          subtitle="Expected profit across current loan book"
        />
        <SummaryCard
          title="Wallet Balance"
          value={wallet?.balance ?? 0}
          subtitle={wallet?.frozen ? "Wallet is frozen" : "Wallet is active"}
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="rounded-2xl bg-white p-6 shadow-sm xl:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-slate-900">Lender Summary</h3>
            <button
              onClick={loadDashboard}
              className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Refresh
            </button>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Total Loans</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.totalLoans ?? 0}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Active Loans</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.activeLoans ?? 0}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Closed Loans</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.closedLoans ?? 0}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Total Outstanding</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.totalOutstanding ?? 0}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Penalty Collected</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.totalPenaltyCollected ?? 0}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Recoverable Amount</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.recoverableAmount ?? 0}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Capital Ready For Reuse</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.capitalReadyForReuse ?? 0}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Overdue Borrowers</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{summary?.overdueBorrowers ?? 0}</h4>
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
                No notifications yet. Later we will wire repayment, blacklist, freeze, approval, and scheduler notifications here.
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

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Today's Repayment Distribution</h3>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={repaymentStatusChartData}
                  dataKey="value"
                  nameKey="name"
                  outerRadius={100}
                  label
                >
                  {repaymentStatusChartData.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={["#2563eb", "#dc2626", "#d97706", "#059669"][index % 4]}
                    />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Loan Plan Performance</h3>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={planChartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" hide={planChartData.length > 4} />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="disbursed" fill="#2563eb" />
                <Bar dataKey="collected" fill="#059669" />
                <Bar dataKey="outstanding" fill="#dc2626" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">Borrower Risk Breakdown</h3>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={borrowerRiskChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" hide={borrowerRiskChartData.length > 5} />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="riskScore" stroke="#d97706" strokeWidth={2} />
              <Line type="monotone" dataKey="outstanding" stroke="#2563eb" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Today's Collections</h3>
          <p className="text-sm text-slate-500">Fetched from lender collection endpoint</p>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Borrower</th>
                <th className="px-3 py-3">Loan ID</th>
                <th className="px-3 py-3">Amount Paid</th>
                <th className="px-3 py-3">Mode</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Date</th>
                <th className="px-3 py-3">Balance</th>
              </tr>
            </thead>
            <tbody>
              {todayCollections.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-3 py-6 text-center text-slate-500">
                    No collections recorded for today.
                  </td>
                </tr>
              ) : (
                todayCollections.map((item, index) => (
                  <tr key={`${item.loanId}-${index}`} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.borrowerName}</td>
                    <td className="px-3 py-3 text-slate-700">{item.loanId}</td>
                    <td className="px-3 py-3 text-slate-700">{item.amountPaid}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentMode}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentStatus}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentDate}</td>
                    <td className="px-3 py-3 text-slate-700">{item.balanceAmount}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Borrower Risk Table</h3>
          <p className="text-sm text-slate-500">Borrowers connected to this lender</p>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Borrower</th>
                <th className="px-3 py-3">Internal Score</th>
                <th className="px-3 py-3">Risk Score</th>
                <th className="px-3 py-3">Risk Category</th>
                <th className="px-3 py-3">Loans</th>
                <th className="px-3 py-3">Outstanding</th>
                <th className="px-3 py-3">Missed Days</th>
                <th className="px-3 py-3">Late Payments</th>
              </tr>
            </thead>
            <tbody>
              {borrowerRiskBreakdown.length === 0 ? (
                <tr>
                  <td colSpan="8" className="px-3 py-6 text-center text-slate-500">
                    No borrower risk records available.
                  </td>
                </tr>
              ) : (
                borrowerRiskBreakdown.map((item) => (
                  <tr key={item.borrowerId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.borrowerName}</td>
                    <td className="px-3 py-3 text-slate-700">{item.internalCreditScore}</td>
                    <td className="px-3 py-3 text-slate-700">{item.riskScore}</td>
                    <td className="px-3 py-3 text-slate-700">{item.riskCategory}</td>
                    <td className="px-3 py-3 text-slate-700">{item.totalLoansWithLender}</td>
                    <td className="px-3 py-3 text-slate-700">{item.currentOutstanding}</td>
                    <td className="px-3 py-3 text-slate-700">{item.totalMissedDays}</td>
                    <td className="px-3 py-3 text-slate-700">{item.totalLatePayments}</td>
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