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

function getRepaymentTone(status) {
  const value = String(status || "").toUpperCase();

  if (value === "FULL" || value === "LATE_FULL") return "green";
  if (value === "ADVANCE" || value === "PRE_CLOSURE") return "blue";
  if (value === "PARTIAL") return "yellow";
  if (value === "MISSED") return "red";

  return "slate";
}

function getCashCollectionTone(status) {
  const value = String(status || "").toUpperCase();

  if (value === "CONFIRMED") return "green";
  if (value === "PENDING_BORROWER_CONFIRMATION") return "yellow";
  if (value === "REJECTED" || value === "EXPIRED") return "red";

  return "slate";
}

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString();
}

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function money(value) {
  const num = Number(value || 0);
  return num.toFixed(2);
}

function normalizeApiData(response) {
  const body = response?.data;

  if (
    body &&
    typeof body === "object" &&
    !Array.isArray(body) &&
    Object.prototype.hasOwnProperty.call(body, "data") &&
    (Object.prototype.hasOwnProperty.call(body, "status") ||
      Object.prototype.hasOwnProperty.call(body, "message"))
  ) {
    return body.data;
  }

  return body;
}

function getTodayInputValue() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export default function BorrowerRepaymentPage() {
  const { userId } = useAuth();

  const [summary, setSummary] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [loans, setLoans] = useState([]);
  const [repaymentsMap, setRepaymentsMap] = useState({});
  const [cashRequests, setCashRequests] = useState([]);

  const [selectedLoanId, setSelectedLoanId] = useState(null);

  const [paymentForm, setPaymentForm] = useState({
    amountPaid: "",
    paymentMode: "WALLET",
  });

  const [confirmationForms, setConfirmationForms] = useState({});

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [pageError, setPageError] = useState("");
  const [message, setMessage] = useState("");

  const selectedLoan = useMemo(() => {
    return loans.find((loan) => loan.loanId === selectedLoanId) || null;
  }, [loans, selectedLoanId]);

  const selectedLoanRepayments = useMemo(() => {
    if (!selectedLoanId) return [];
    return repaymentsMap[selectedLoanId] || [];
  }, [repaymentsMap, selectedLoanId]);

  const pendingCashConfirmations = useMemo(() => {
    return cashRequests.filter((item) => item.status === "PENDING_BORROWER_CONFIRMATION");
  }, [cashRequests]);

  const activeLoansCount = useMemo(() => {
    return loans.filter((loan) => loan.isClosed !== true).length;
  }, [loans]);

  const totalOutstanding = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.remainingAmount || 0), 0).toFixed(2);
  }, [loans]);

  const totalOverdue = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.overdueAmount || 0), 0).toFixed(2);
  }, [loans]);

  const totalInterestRebate = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.totalInterestRebateAmount || 0), 0).toFixed(2);
  }, [loans]);

  const totalRepaid = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.totalPaidAmount || 0), 0).toFixed(2);
  }, [loans]);

  const unreadCount = useMemo(() => {
    return notifications.filter((item) => !item.readFlag).length;
  }, [notifications]);

  async function loadPage(showMessage = false) {
    if (!userId) return;

    setLoading(true);
    setPageError("");

    try {
      const [summaryResponse, walletResponse, loansResponse, notificationsResponse, cashResponse] = await Promise.all([
        api.get(`/api/borrower-analytics/${userId}/summary`),
        api.get(`/api/wallets/BORROWER/${userId}`),
        api.get(`/api/loans/borrower/${userId}`),
        api.get(`/api/notifications/BORROWER/${userId}`),
        api.get(`/api/cash-collections/borrower/${userId}`),
      ]);

      const borrowerSummary = normalizeApiData(summaryResponse) || null;
      const borrowerWallet = normalizeApiData(walletResponse) || null;
      const borrowerLoans = normalizeApiData(loansResponse) || [];
      const borrowerNotifications = normalizeApiData(notificationsResponse) || [];
      const borrowerCashRequests = normalizeApiData(cashResponse) || [];

      setSummary(borrowerSummary);
      setWallet(borrowerWallet);
      setLoans(borrowerLoans);
      setNotifications(borrowerNotifications);
      setCashRequests(borrowerCashRequests);

      const repaymentsEntries = await Promise.all(
        borrowerLoans.map(async (loan) => {
          try {
            const response = await api.get(`/api/repayments/loan/${loan.loanId}`);
            return [loan.loanId, normalizeApiData(response) || []];
          } catch {
            return [loan.loanId, []];
          }
        })
      );

      const repaymentsObj = Object.fromEntries(repaymentsEntries);
      setRepaymentsMap(repaymentsObj);

      if (!selectedLoanId && borrowerLoans.length > 0) {
        setSelectedLoanId(borrowerLoans[0].loanId);
      } else if (
        selectedLoanId &&
        borrowerLoans.length > 0 &&
        !borrowerLoans.some((loan) => loan.loanId === selectedLoanId)
      ) {
        setSelectedLoanId(borrowerLoans[0].loanId);
      }

      if (showMessage) {
        setMessage("Borrower repayment data refreshed successfully.");
      }
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load borrower repayment data.");
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

  async function handleRepaymentSubmit(event) {
    event.preventDefault();

    if (!selectedLoan) {
      setPageError("Please select a loan first.");
      return;
    }

    if (!paymentForm.amountPaid || Number(paymentForm.amountPaid) < 0) {
      setPageError("Please enter a valid repayment amount.");
      return;
    }

    setSubmitting(true);
    setPageError("");
    setMessage("");

    try {
      const payload = {
        loanId: selectedLoan.loanId,
        loanApplicationId: selectedLoan.loanApplicationId,
        amountPaid: Number(paymentForm.amountPaid),
        paymentMode: paymentForm.paymentMode,
        paymentDate: null,
      };

      const response = await api.post(`/api/repayments`, payload);
      const repaymentResult = normalizeApiData(response) || response.data;

      setMessage(
        response.data?.message ||
          `Repayment recorded successfully. Status: ${repaymentResult?.paymentStatus || "UPDATED"}`
      );

      setPaymentForm({
        amountPaid: selectedLoan.dailyEmi ? String(selectedLoan.dailyEmi) : "",
        paymentMode: "WALLET",
      });

      await loadPage(false);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to record repayment.");
    } finally {
      setSubmitting(false);
    }
  }

  function updateConfirmationForm(confirmationId, field, value) {
    setConfirmationForms((prev) => ({
      ...prev,
      [confirmationId]: {
        ...(prev[confirmationId] || { confirmationToken: "", borrowerNote: "" }),
        [field]: value,
      },
    }));
  }

  async function confirmCashCollection(confirmationId) {
    const form = confirmationForms[confirmationId] || { confirmationToken: "", borrowerNote: "" };

    if (!form.confirmationToken || form.confirmationToken.trim().length < 3) {
      setPageError("Please enter the repayment proof token before confirming.");
      return;
    }

    setActionLoadingId(confirmationId);
    setPageError("");
    setMessage("");

    try {
      const payload = {
        confirmationToken: form.confirmationToken.trim(),
        borrowerNote: form.borrowerNote?.trim() || null,
      };

      const response = await api.post(`/api/cash-collections/borrower/${userId}/${confirmationId}/confirm`, payload);
      setMessage(response.data?.message || "Cash repayment confirmed successfully.");
      await loadPage(false);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to confirm cash repayment.");
    } finally {
      setActionLoadingId(null);
    }
  }

  async function rejectCashCollection(confirmationId) {
    const form = confirmationForms[confirmationId] || { borrowerNote: "" };

    setActionLoadingId(confirmationId);
    setPageError("");
    setMessage("");

    try {
      const payload = {
        borrowerNote: form.borrowerNote?.trim() || null,
      };

      const response = await api.post(`/api/cash-collections/borrower/${userId}/${confirmationId}/reject`, payload);
      setMessage(response.data?.message || "Cash repayment rejected successfully.");
      await loadPage(false);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to reject cash repayment.");
    } finally {
      setActionLoadingId(null);
    }
  }

  useEffect(() => {
    loadPage(false);
  }, [userId]);

  useEffect(() => {
    if (selectedLoan) {
      setPaymentForm({
        amountPaid: selectedLoan.dailyEmi ? String(selectedLoan.dailyEmi) : "",
        paymentMode: "WALLET",
      });
    }
  }, [selectedLoanId, selectedLoan]);

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading borrower repayment page...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Borrower Repayment Center</h2>
        <p className="mt-2 text-sm text-slate-500">
          Review your active loans, confirm cash repayment proof requests, see effective repayable balance, and make repayments.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3 xl:grid-cols-7">
        <SummaryCard title="Total Loans" value={loans.length} subtitle="All borrower loans" />
        <SummaryCard title="Active Loans" value={activeLoansCount} subtitle="Still open" />
        <SummaryCard title="Wallet Balance" value={money(wallet?.balance)} subtitle={wallet?.frozen ? "Wallet is frozen" : "Wallet is active"} />
        <SummaryCard title="Total Repaid" value={totalRepaid} subtitle="Across all loans" />
        <SummaryCard title="Outstanding" value={totalOutstanding} subtitle="Remaining repayable balance" />
        <SummaryCard title="Interest Rebate" value={totalInterestRebate} subtitle="Saved by advance / early repayment" />
        <SummaryCard title="Pending Proof Requests" value={pendingCashConfirmations.length} subtitle="Cash confirmations awaiting you" />
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard title="Overdue Amount" value={totalOverdue} subtitle="Current overdue across loans" />
        <SummaryCard title="Risk Category" value={summary?.riskCategory || "UNKNOWN"} subtitle={`Risk Score: ${summary?.riskScore ?? 0}`} />
        <SummaryCard title="Unread Notifications" value={unreadCount} subtitle="Borrower alerts" />
      </div>

      {(pageError || message) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          {message ? (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div>
          ) : null}
          {pageError ? (
            <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{pageError}</div>
          ) : null}
        </div>
      )}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Cash Repayment Proof Requests</h3>
          <button
            type="button"
            onClick={() => loadPage(true)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
          >
            Refresh
          </button>
        </div>

        <div className="space-y-4">
          {cashRequests.length === 0 ? (
            <p className="text-sm text-slate-500">No cash repayment proof requests found.</p>
          ) : (
            cashRequests.map((item) => {
              const form = confirmationForms[item.confirmationId] || { confirmationToken: "", borrowerNote: "" };
              const isPending = item.status === "PENDING_BORROWER_CONFIRMATION";

              return (
                <div key={item.confirmationId} className="rounded-2xl border border-slate-200 p-5">
                  <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <h4 className="text-base font-semibold text-slate-900">Loan {item.loanId} Cash Proof Request</h4>
                      <p className="mt-1 text-sm text-slate-500">Proof token: <span className="font-semibold text-slate-700">{item.generatedToken || "-"}</span></p>
                    </div>
                    <StatusPill text={item.status} tone={getCashCollectionTone(item.status)} />
                  </div>

                  <div className="grid grid-cols-1 gap-3 text-sm text-slate-700 md:grid-cols-3">
                    <p><span className="font-medium">Amount:</span> {item.amount}</p>
                    <p><span className="font-medium">Payment Date:</span> {formatDate(item.paymentDate)}</p>
                    <p><span className="font-medium">Repayment ID:</span> {item.repaymentId || "-"}</p>
                    <p><span className="font-medium">Lender Note:</span> {item.lenderNote || "-"}</p>
                    <p><span className="font-medium">Borrower Note:</span> {item.borrowerNote || "-"}</p>
                    <p><span className="font-medium">Expires At:</span> {formatDateTime(item.expiresAt)}</p>
                  </div>

                  {isPending ? (
                    <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
                      <div>
                        <label className="mb-1 block text-sm font-medium text-slate-700">Enter Proof Token</label>
                        <input
                          type="text"
                          value={form.confirmationToken}
                          onChange={(e) => updateConfirmationForm(item.confirmationId, "confirmationToken", e.target.value)}
                          className="w-full rounded-xl border border-slate-300 px-3 py-2"
                          placeholder="Enter lender-shared proof token"
                        />
                      </div>

                      <div>
                        <label className="mb-1 block text-sm font-medium text-slate-700">Borrower Note</label>
                        <input
                          type="text"
                          value={form.borrowerNote}
                          onChange={(e) => updateConfirmationForm(item.confirmationId, "borrowerNote", e.target.value)}
                          className="w-full rounded-xl border border-slate-300 px-3 py-2"
                          placeholder="Optional note"
                        />
                      </div>

                      <div className="md:col-span-2 flex flex-wrap gap-3">
                        <button
                          type="button"
                          disabled={actionLoadingId === item.confirmationId}
                          onClick={() => confirmCashCollection(item.confirmationId)}
                          className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
                        >
                          {actionLoadingId === item.confirmationId ? "Processing..." : "Confirm Cash Repayment"}
                        </button>

                        <button
                          type="button"
                          disabled={actionLoadingId === item.confirmationId}
                          onClick={() => rejectCashCollection(item.confirmationId)}
                          className="rounded-xl border border-red-300 px-4 py-2 text-sm font-medium text-red-700 disabled:opacity-60"
                        >
                          {actionLoadingId === item.confirmationId ? "Processing..." : "Reject Request"}
                        </button>
                      </div>
                    </div>
                  ) : null}
                </div>
              );
            })
          )}
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">My Loans</h3>
          <button
            type="button"
            onClick={() => loadPage(true)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
          >
            Refresh
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Loan ID</th>
                <th className="px-3 py-3">Principal</th>
                <th className="px-3 py-3">Disbursed</th>
                <th className="px-3 py-3">Contract Repayable</th>
                <th className="px-3 py-3">Effective Repayable</th>
                <th className="px-3 py-3">Interest Rebate</th>
                <th className="px-3 py-3">Outstanding</th>
                <th className="px-3 py-3">Overdue</th>
                <th className="px-3 py-3">Daily EMI</th>
                <th className="px-3 py-3">Next Due</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {loans.length === 0 ? (
                <tr>
                  <td colSpan="12" className="px-3 py-6 text-center text-slate-500">
                    No borrower loans found.
                  </td>
                </tr>
              ) : (
                loans.map((loan) => (
                  <tr key={loan.loanId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{loan.loanId}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.sanctionedAmount ?? loan.totalAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.disbursedAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.totalRepayableAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.effectiveTotalRepayableAmount ?? loan.totalRepayableAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.totalInterestRebateAmount ?? 0}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.remainingAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.overdueAmount ?? 0}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.dailyEmi ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{formatDate(loan.nextDueDate)}</td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill
                        text={loan.isClosed ? "CLOSED" : "ACTIVE"}
                        tone={loan.isClosed ? "green" : "blue"}
                      />
                    </td>
                    <td className="px-3 py-3">
                      <button
                        type="button"
                        onClick={() => setSelectedLoanId(loan.loanId)}
                        className="rounded-xl border border-slate-300 px-3 py-2 text-xs font-medium text-slate-700"
                      >
                        {selectedLoanId === loan.loanId ? "Selected" : "Select"}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {selectedLoan ? (
        <>
          <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
            <div className="rounded-2xl bg-white p-6 shadow-sm">
              <h3 className="mb-4 text-lg font-semibold text-slate-900">Selected Loan Summary</h3>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Loan ID</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">{selectedLoan.loanId}</h4>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Daily EMI</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">{selectedLoan.dailyEmi ?? "-"}</h4>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Contract Repayable</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">{selectedLoan.totalRepayableAmount ?? "-"}</h4>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Effective Repayable</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">
                    {selectedLoan.effectiveTotalRepayableAmount ?? selectedLoan.totalRepayableAmount ?? "-"}
                  </h4>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Daily Interest Amount</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">{selectedLoan.dailyInterestAmount ?? 0}</h4>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Interest Rebate Saved</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">{selectedLoan.totalInterestRebateAmount ?? 0}</h4>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Outstanding</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">{selectedLoan.remainingAmount ?? "-"}</h4>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Overdue</p>
                  <h4 className="mt-2 text-xl font-semibold text-slate-900">{selectedLoan.overdueAmount ?? 0}</h4>
                </div>
              </div>
            </div>

            <div className="rounded-2xl bg-white p-6 shadow-sm">
              <h3 className="mb-4 text-lg font-semibold text-slate-900">Make Repayment</h3>

              <form onSubmit={handleRepaymentSubmit} className="space-y-4">
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Amount Paid</label>
                  <input
                    type="number"
                    step="0.01"
                    value={paymentForm.amountPaid}
                    onChange={(e) =>
                      setPaymentForm((prev) => ({ ...prev, amountPaid: e.target.value }))
                    }
                    className="w-full rounded-xl border border-slate-300 px-3 py-2"
                    placeholder="Enter repayment amount"
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    Use a higher amount for advance payment or pre-closure. Interest rebate will be applied automatically when eligible.
                  </p>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Payment Mode</label>
                  <select
                    value={paymentForm.paymentMode}
                    onChange={(e) =>
                      setPaymentForm((prev) => ({ ...prev, paymentMode: e.target.value }))
                    }
                    className="w-full rounded-xl border border-slate-300 px-3 py-2"
                  >
                    <option value="WALLET">WALLET</option>
                    <option value="CASH">CASH</option>
                  </select>
                  <p className="mt-1 text-xs text-slate-500">
                    Wallet mode directly updates borrower wallet. Cash mode records a cash repayment entry in the ledger.
                  </p>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Payment Date</label>
                  <input
                    type="text"
                    value={getTodayInputValue()}
                    readOnly
                    className="w-full rounded-xl border border-slate-300 bg-slate-100 px-3 py-2 text-slate-600"
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    Repayments from this page are always recorded for today.
                  </p>
                </div>

                <button
                  type="submit"
                  disabled={submitting || selectedLoan?.isClosed}
                  className="w-full rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white disabled:opacity-60"
                >
                  {submitting ? "Submitting Repayment..." : selectedLoan?.isClosed ? "Loan Already Closed" : "Submit Repayment"}
                </button>
              </form>
            </div>
          </div>

          <div className="rounded-2xl bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-lg font-semibold text-slate-900">Repayment History For Selected Loan</h3>

            <div className="overflow-x-auto">
              <table className="min-w-full border-collapse text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="px-3 py-3">Repayment ID</th>
                    <th className="px-3 py-3">Date</th>
                    <th className="px-3 py-3">Amount</th>
                    <th className="px-3 py-3">Mode</th>
                    <th className="px-3 py-3">Status</th>
                    <th className="px-3 py-3">Penalty</th>
                    <th className="px-3 py-3">Interest Rebate</th>
                    <th className="px-3 py-3">Missed Days</th>
                    <th className="px-3 py-3">Balance</th>
                    <th className="px-3 py-3">Reference</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedLoanRepayments.length === 0 ? (
                    <tr>
                      <td colSpan="10" className="px-3 py-6 text-center text-slate-500">
                        No repayments recorded for this loan yet.
                      </td>
                    </tr>
                  ) : (
                    selectedLoanRepayments.map((item) => (
                      <tr key={item.id} className="border-b border-slate-100">
                        <td className="px-3 py-3 text-slate-700">{item.id}</td>
                        <td className="px-3 py-3 text-slate-700">{formatDate(item.paymentDate)}</td>
                        <td className="px-3 py-3 text-slate-700">{item.amountPaid}</td>
                        <td className="px-3 py-3 text-slate-700">{item.paymentMode}</td>
                        <td className="px-3 py-3 text-slate-700">
                          <StatusPill text={item.paymentStatus || "UNKNOWN"} tone={getRepaymentTone(item.paymentStatus)} />
                        </td>
                        <td className="px-3 py-3 text-slate-700">{item.penaltyAmount ?? 0}</td>
                        <td className="px-3 py-3 text-slate-700">{item.interestRebateApplied ?? 0}</td>
                        <td className="px-3 py-3 text-slate-700">{item.missedDays ?? 0}</td>
                        <td className="px-3 py-3 text-slate-700">{item.balanceAmount ?? "-"}</td>
                        <td className="px-3 py-3 text-slate-700">{item.transactionReference || "-"}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : null}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Notifications</h3>
          <StatusPill text={`${unreadCount} unread`} tone={unreadCount > 0 ? "blue" : "slate"} />
        </div>

        <div className="space-y-3">
          {notifications.length === 0 ? (
            <p className="text-sm text-slate-500">No notifications yet.</p>
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
                    <p className="mt-2 text-xs text-slate-500">{formatDateTime(item.createdAt)}</p>
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
  );
}
