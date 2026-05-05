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

function getApiPayload(response) {
  const body = response?.data;
  if (
    body &&
    typeof body === "object" &&
    !Array.isArray(body) &&
    Object.prototype.hasOwnProperty.call(body, "data") &&
    (
      Object.prototype.hasOwnProperty.call(body, "status") ||
      Object.prototype.hasOwnProperty.call(body, "message")
    )
  ) {
    return body.data;
  }
  return body;
}

function toNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function money(value) {
  return toNumber(value).toFixed(2);
}

function percent(value) {
  return `${toNumber(value).toFixed(2)}%`;
}

function formatDateTime(dateValue) {
  if (!dateValue) return "-";
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return String(dateValue);
  return date.toLocaleString();
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

  const unreadCount = useMemo(
    () => notifications.filter((item) => !item.readFlag).length,
    [notifications]
  );

  const metrics = useMemo(() => {
    const totalPrincipalDisbursed =
      toNumber(summary?.totalPrincipalDisbursed) ||
      toNumber(summary?.totalDisbursed);

    const totalCollected = toNumber(summary?.totalCollected);

    const expectedTotalRepayment =
      toNumber(summary?.expectedTotalRepayment) ||
      toNumber(summary?.recoverableAmount);

    const currentOutstanding =
      toNumber(summary?.currentOutstanding) ||
      toNumber(summary?.totalOutstanding);

    const totalOverdue =
      toNumber(summary?.totalOverdue) ||
      toNumber(summary?.overdueAmount);

    const grossProfit =
      toNumber(summary?.estimatedGrossProfit) ||
      toNumber(summary?.projectedProfit);

    const platformFee =
      toNumber(summary?.estimatedPlatformFee) ||
      toNumber(summary?.platformFeeAmount);

    const netProfit =
      toNumber(summary?.estimatedNetProfit) ||
      Math.max(0, grossProfit - platformFee);

    const totalPenaltyAccrued =
      toNumber(summary?.totalPenaltyAccrued) ||
      toNumber(summary?.totalPenaltyCollected);

    const recoveryRate =
      toNumber(summary?.projectedRecoveryRate) ||
      (totalPrincipalDisbursed > 0
        ? (totalCollected / totalPrincipalDisbursed) * 100
        : 0);

    const collectionRate =
      toNumber(summary?.projectedCollectionRate) ||
      (expectedTotalRepayment > 0
        ? (totalCollected / expectedTotalRepayment) * 100
        : 0);

    const paidToday =
      toNumber(summary?.paidTodayCount) ||
      toNumber(summary?.paidToday);

    const missedToday =
      toNumber(summary?.missedTodayCount) ||
      toNumber(summary?.missedToday);

    const partialToday =
      toNumber(summary?.partialTodayCount) ||
      toNumber(summary?.partialToday);

    const advanceToday =
      toNumber(summary?.advanceTodayCount) ||
      toNumber(summary?.advanceToday);

    const overdueBorrowers =
      toNumber(summary?.currentlyOverdueBorrowerCount) ||
      toNumber(summary?.overdueBorrowers);

    return {
      totalLoans: toNumber(summary?.totalLoans),
      activeLoans: toNumber(summary?.activeLoans),
      closedLoans: toNumber(summary?.closedLoans),
      defaultedLoans: toNumber(summary?.defaultedLoans),
      totalPrincipalDisbursed,
      totalCollected,
      expectedTotalRepayment,
      currentOutstanding,
      totalOverdue,
      grossProfit,
      platformFee,
      netProfit,
      totalPenaltyAccrued,
      recoveryRate,
      collectionRate,
      paidToday,
      missedToday,
      partialToday,
      advanceToday,
      overdueBorrowers,
      averageExpectedReturnPerLoan: toNumber(summary?.averageExpectedReturnPerLoan),
    };
  }, [summary]);

  const repaymentStatusChartData = useMemo(() => {
    return [
      { name: "Paid Today", value: metrics.paidToday },
      { name: "Missed Today", value: metrics.missedToday },
      { name: "Partial Today", value: metrics.partialToday },
      { name: "Advance Today", value: metrics.advanceToday },
    ];
  }, [metrics]);

  const planChartData = useMemo(() => {
    return planPerformance.map((item) => ({
      name: item.planName || `Plan ${item.planId || ""}`,
      disbursed: toNumber(item.totalDisbursed),
      collected: toNumber(item.totalCollected),
      outstanding: toNumber(item.totalOutstanding),
      projectedProfit: toNumber(item.projectedProfit),
    }));
  }, [planPerformance]);

  const borrowerRiskChartData = useMemo(() => {
    return borrowerRiskBreakdown.map((item) => ({
      name: item.borrowerName,
      riskScore: toNumber(item.riskScore),
      outstanding: toNumber(item.currentOutstanding),
    }));
  }, [borrowerRiskBreakdown]);

  const delinquentBorrowers = useMemo(() => {
    return [...borrowerRiskBreakdown]
      .filter(
        (item) =>
          toNumber(item.currentOutstanding) > 0 ||
          toNumber(item.totalMissedDays) > 0 ||
          toNumber(item.totalLatePayments) > 0
      )
      .sort((a, b) => {
        const overdueCompare = toNumber(b.currentOutstanding) - toNumber(a.currentOutstanding);
        if (overdueCompare !== 0) return overdueCompare;
        return toNumber(b.totalMissedDays) - toNumber(a.totalMissedDays);
      });
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

      const [
        summaryResult,
        todayCollectionsResult,
        planPerformanceResult,
        borrowerRiskResult,
        walletResult,
        notificationsResult,
      ] = results;

      if (summaryResult.status === "fulfilled") {
        setSummary(getApiPayload(summaryResult.value) || null);
      }

      if (todayCollectionsResult.status === "fulfilled") {
        setTodayCollections(getApiPayload(todayCollectionsResult.value) || []);
      }

      if (planPerformanceResult.status === "fulfilled") {
        setPlanPerformance(getApiPayload(planPerformanceResult.value) || []);
      }

      if (borrowerRiskResult.status === "fulfilled") {
        setBorrowerRiskBreakdown(getApiPayload(borrowerRiskResult.value) || []);
      }

      if (walletResult.status === "fulfilled") {
        setWallet(getApiPayload(walletResult.value) || null);
      }

      if (notificationsResult.status === "fulfilled") {
        setNotifications(getApiPayload(notificationsResult.value) || []);
      }

      const allFailed = results.every((result) => result.status === "rejected");
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
          This dashboard shows lender portfolio analytics, recovery performance, borrower risk, wallet state, and today’s collection activity.
        </p>

        {error ? (
          <div className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
            {error}
          </div>
        ) : null}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Total Principal Disbursed"
          value={money(metrics.totalPrincipalDisbursed)}
          subtitle="Total amount released to borrowers"
        />
        <SummaryCard
          title="Total Collected"
          value={money(metrics.totalCollected)}
          subtitle="All repayments received so far"
        />
        <SummaryCard
          title="Current Outstanding"
          value={money(metrics.currentOutstanding)}
          subtitle="Still pending from active loans"
        />
        <SummaryCard
          title="Wallet Balance"
          value={money(wallet?.balance)}
          subtitle={wallet?.frozen ? "Wallet is frozen" : "Wallet is active"}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Expected Total Repayment"
          value={money(metrics.expectedTotalRepayment)}
          subtitle="Portfolio expected collection"
        />
        <SummaryCard
          title="Gross Profit Forecast"
          value={money(metrics.grossProfit)}
          subtitle="Before platform fee deduction"
        />
        <SummaryCard
          title="Platform Fee"
          value={money(metrics.platformFee)}
          subtitle="Expected or accrued platform share"
        />
        <SummaryCard
          title="Net Profit Forecast"
          value={money(metrics.netProfit)}
          subtitle="Estimated lender-side profit"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3 xl:grid-cols-6">
        <SummaryCard
          title="Total Loans"
          value={metrics.totalLoans}
          subtitle="All loans created by this lender"
        />
        <SummaryCard
          title="Active Loans"
          value={metrics.activeLoans}
          subtitle="Still open for recovery"
        />
        <SummaryCard
          title="Closed Loans"
          value={metrics.closedLoans}
          subtitle="Completed loans"
        />
        <SummaryCard
          title="Overdue Borrowers"
          value={metrics.overdueBorrowers}
          subtitle="Borrowers needing attention"
        />
        <SummaryCard
          title="Recovery Rate"
          value={percent(metrics.recoveryRate)}
          subtitle="Collected / principal disbursed"
        />
        <SummaryCard
          title="Collection Rate"
          value={percent(metrics.collectionRate)}
          subtitle="Collected / expected repayment"
        />
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3 xl:grid-cols-6">
        <SummaryCard
          title="Paid Today"
          value={metrics.paidToday}
          subtitle="Full / late-full collections"
        />
        <SummaryCard
          title="Missed Today"
          value={metrics.missedToday}
          subtitle="Missed repayment events"
        />
        <SummaryCard
          title="Partial Today"
          value={metrics.partialToday}
          subtitle="Partial payment count"
        />
        <SummaryCard
          title="Advance Today"
          value={metrics.advanceToday}
          subtitle="Advance / pre-closure events"
        />
        <SummaryCard
          title="Penalty Accrued"
          value={money(metrics.totalPenaltyAccrued)}
          subtitle="Penalty total in portfolio"
        />
        <SummaryCard
          title="Avg Profit / Loan"
          value={money(metrics.averageExpectedReturnPerLoan)}
          subtitle="Average expected return"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="rounded-2xl bg-white p-6 shadow-sm xl:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-slate-900">Portfolio Summary</h3>
            <button
              onClick={loadDashboard}
              className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Refresh
            </button>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Total Principal Disbursed</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{money(metrics.totalPrincipalDisbursed)}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Total Collected</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{money(metrics.totalCollected)}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Expected Total Repayment</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{money(metrics.expectedTotalRepayment)}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Current Outstanding</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{money(metrics.currentOutstanding)}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Total Overdue</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{money(metrics.totalOverdue)}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Overdue Borrowers</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{metrics.overdueBorrowers}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Gross Profit Forecast</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{money(metrics.grossProfit)}</h4>
            </div>

            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Net Profit Forecast</p>
              <h4 className="mt-2 text-xl font-semibold text-slate-900">{money(metrics.netProfit)}</h4>
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
                No notifications yet.
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
                        {item.createdAt ? formatDateTime(item.createdAt) : ""}
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
                    <td className="px-3 py-3 text-slate-700">{money(item.amountPaid)}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentMode}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentStatus}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentDate}</td>
                    <td className="px-3 py-3 text-slate-700">{money(item.balanceAmount)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Delinquent Borrowers</h3>
          <p className="text-sm text-slate-500">People who missed payments / still owe money</p>
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
              {delinquentBorrowers.length === 0 ? (
                <tr>
                  <td colSpan="8" className="px-3 py-6 text-center text-slate-500">
                    No delinquent borrower records available.
                  </td>
                </tr>
              ) : (
                delinquentBorrowers.map((item) => (
                  <tr key={item.borrowerId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.borrowerName}</td>
                    <td className="px-3 py-3 text-slate-700">{item.internalCreditScore}</td>
                    <td className="px-3 py-3 text-slate-700">{item.riskScore}</td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill
                        text={item.riskCategory}
                        tone={
                          item.riskCategory === "HIGH"
                            ? "red"
                            : item.riskCategory === "MEDIUM"
                            ? "yellow"
                            : "green"
                        }
                      />
                    </td>
                    <td className="px-3 py-3 text-slate-700">{item.totalLoansWithLender}</td>
                    <td className="px-3 py-3 text-slate-700">{money(item.currentOutstanding)}</td>
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