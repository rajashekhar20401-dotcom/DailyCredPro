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

function formatDate(dateValue) {
  if (!dateValue) return "-";
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return String(dateValue);
  return date.toLocaleDateString();
}

function getRepaymentTone(status) {
  if (status === "FULL" || status === "ADVANCE" || status === "PRE_CLOSURE") return "green";
  if (status === "PARTIAL" || status === "LATE_FULL") return "yellow";
  if (status === "MISSED") return "red";
  return "slate";
}

function getCashCollectionTone(status) {
  if (status === "CONFIRMED") return "green";
  if (status === "REJECTED" || status === "EXPIRED") return "red";
  if (status === "PENDING_BORROWER_CONFIRMATION") return "yellow";
  return "slate";
}

export default function BorrowerRepaymentPage() {
  const { userId } = useAuth();

  const [loans, setLoans] = useState([]);
  const [selectedLoan, setSelectedLoan] = useState(null);
  const [repayments, setRepayments] = useState([]);
  const [cashRequests, setCashRequests] = useState([]);

  const [form, setForm] = useState({
    amountPaid: "",
    paymentMode: "WALLET",
    paymentDate: "",
  });

  const [tokenInputs, setTokenInputs] = useState({});
  const [noteInputs, setNoteInputs] = useState({});

  const [loadingLoans, setLoadingLoans] = useState(true);
  const [loadingRepayments, setLoadingRepayments] = useState(false);
  const [loadingCashRequests, setLoadingCashRequests] = useState(false);
  const [paying, setPaying] = useState(false);
  const [processingCashActionId, setProcessingCashActionId] = useState(null);

  const [message, setMessage] = useState("");
  const [pageError, setPageError] = useState("");

  const activeLoans = useMemo(() => {
    return loans.filter((loan) => loan.isClosed !== true);
  }, [loans]);

  const pendingCashRequests = useMemo(() => {
    return cashRequests.filter((item) => item.status === "PENDING_BORROWER_CONFIRMATION");
  }, [cashRequests]);

  async function loadBorrowerLoans(showMessage = false) {
    if (!userId) return;

    setLoadingLoans(true);
    setPageError("");

    try {
      const response = await api.get(`/api/loans/borrower/${userId}`);
      const loanList = response.data?.data || [];
      setLoans(loanList);

      if (showMessage) {
        setMessage("Loans refreshed successfully.");
      }
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load borrower loans.");
    } finally {
      setLoadingLoans(false);
    }
  }

  async function loadCashRequests() {
    if (!userId) return;

    setLoadingCashRequests(true);

    try {
      const response = await api.get(`/api/cash-collections/borrower/${userId}`);
      setCashRequests(response.data?.data || []);
    } catch (err) {
      setCashRequests([]);
    } finally {
      setLoadingCashRequests(false);
    }
  }

  async function selectLoan(loan) {
    setSelectedLoan(loan);
    setMessage("");
    setPageError("");

    setForm({
      amountPaid: loan.dailyEmi ? String(loan.dailyEmi) : "",
      paymentMode: "WALLET",
      paymentDate: "",
    });

    setLoadingRepayments(true);

    try {
      const response = await api.get(`/api/repayments/loan/${loan.loanId}`);
      setRepayments(response.data || []);
    } catch (err) {
      setRepayments([]);
      setPageError(err.response?.data?.message || err.message || "Failed to load repayment history.");
    } finally {
      setLoadingRepayments(false);
    }
  }

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  async function refreshSelectedLoanData(loanId) {
    const updatedLoanResponse = await api.get(`/api/loans/${loanId}`);
    const updatedLoan = updatedLoanResponse.data?.data || null;
    if (updatedLoan) {
      setSelectedLoan(updatedLoan);
    }

    const repaymentHistoryResponse = await api.get(`/api/repayments/loan/${loanId}`);
    setRepayments(repaymentHistoryResponse.data || []);
  }

  async function handleRepayment(event) {
    event.preventDefault();

    if (!selectedLoan) {
      setPageError("Please select a loan first.");
      return;
    }

    setPaying(true);
    setMessage("");
    setPageError("");

    try {
      const payload = {
        loanId: selectedLoan.loanId,
        loanApplicationId: selectedLoan.loanApplicationId,
        amountPaid: form.amountPaid === "" ? null : Number(form.amountPaid),
        paymentMode: form.paymentMode,
        paymentDate: form.paymentDate ? form.paymentDate : null,
      };

      const response = await api.post(`/api/repayments`, payload);
      const repaymentResult = response.data;

      setMessage(
        `Repayment recorded successfully. Status: ${repaymentResult?.paymentStatus || "SUCCESS"}`
      );

      await loadBorrowerLoans(false);
      await loadCashRequests();
      await refreshSelectedLoanData(selectedLoan.loanId);

      setForm({
        amountPaid: selectedLoan?.dailyEmi ? String(selectedLoan.dailyEmi) : "",
        paymentMode: "WALLET",
        paymentDate: "",
      });
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to record repayment.");
    } finally {
      setPaying(false);
    }
  }

  async function handleConfirmCashRequest(confirmationId, loanId) {
    const token = tokenInputs[confirmationId];
    const borrowerNote = noteInputs[confirmationId];

    if (!token || token.trim() === "") {
      setPageError("Please enter the confirmation token before confirming.");
      return;
    }

    setProcessingCashActionId(confirmationId);
    setMessage("");
    setPageError("");

    try {
      const response = await api.post(
        `/api/cash-collections/borrower/${userId}/${confirmationId}/confirm`,
        {
          confirmationToken: token,
          borrowerNote: borrowerNote || null,
        }
      );

      setMessage(response.data?.message || "Cash collection confirmed successfully.");

      await loadBorrowerLoans(false);
      await loadCashRequests();

      if (selectedLoan && selectedLoan.loanId === loanId) {
        await refreshSelectedLoanData(loanId);
      }
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to confirm cash collection.");
    } finally {
      setProcessingCashActionId(null);
    }
  }

  async function handleRejectCashRequest(confirmationId) {
    const borrowerNote = noteInputs[confirmationId];

    setProcessingCashActionId(confirmationId);
    setMessage("");
    setPageError("");

    try {
      const response = await api.post(
        `/api/cash-collections/borrower/${userId}/${confirmationId}/reject`,
        {
          borrowerNote: borrowerNote || null,
        }
      );

      setMessage(response.data?.message || "Cash collection rejected successfully.");
      await loadCashRequests();
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to reject cash collection.");
    } finally {
      setProcessingCashActionId(null);
    }
  }

  useEffect(() => {
    loadBorrowerLoans(false);
    loadCashRequests();
  }, [userId]);

  if (loadingLoans) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading borrower repayments center...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Repay Loan</h2>
        <p className="mt-2 text-sm text-slate-500">
          Repay through wallet directly, or confirm lender-recorded cash collection requests using the shared token.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <SummaryCard title="Total Loans" value={loans.length} subtitle="All loans linked to this borrower" />
        <SummaryCard title="Active Loans" value={activeLoans.length} subtitle="Loans still open for repayment" />
        <SummaryCard title="Selected Loan" value={selectedLoan ? selectedLoan.loanId : "-"} subtitle="Choose one loan to repay" />
        <SummaryCard title="Pending Cash Requests" value={pendingCashRequests.length} subtitle="Awaiting your token confirmation" />
      </div>

      {(message || pageError) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          {message ? (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {message}
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
        <h3 className="mb-4 text-lg font-semibold text-slate-900">Pending Cash Collection Confirmations</h3>

        {loadingCashRequests ? (
          <p className="text-sm text-slate-500">Loading cash confirmations...</p>
        ) : pendingCashRequests.length === 0 ? (
          <p className="text-sm text-slate-500">No pending cash collection confirmations right now.</p>
        ) : (
          <div className="space-y-4">
            {pendingCashRequests.map((item) => (
              <div key={item.confirmationId} className="rounded-2xl border border-slate-200 p-5">
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h4 className="text-lg font-semibold text-slate-900">
                      Cash Confirmation #{item.confirmationId}
                    </h4>
                    <p className="text-sm text-slate-500">
                      Loan #{item.loanId} | Amount: {item.amount} | Payment Date: {formatDate(item.paymentDate)}
                    </p>
                  </div>

                  <StatusPill text={item.status} tone={getCashCollectionTone(item.status)} />
                </div>

                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-slate-700">Confirmation Token</label>
                    <input
                      value={tokenInputs[item.confirmationId] || ""}
                      onChange={(e) =>
                        setTokenInputs((prev) => ({
                          ...prev,
                          [item.confirmationId]: e.target.value,
                        }))
                      }
                      className="w-full rounded-xl border border-slate-300 px-3 py-2"
                      placeholder="Enter 6-digit token shared by lender"
                    />
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-slate-700">Borrower Note (optional)</label>
                    <input
                      value={noteInputs[item.confirmationId] || ""}
                      onChange={(e) =>
                        setNoteInputs((prev) => ({
                          ...prev,
                          [item.confirmationId]: e.target.value,
                        }))
                      }
                      className="w-full rounded-xl border border-slate-300 px-3 py-2"
                      placeholder="Optional note for lender/admin"
                    />
                  </div>
                </div>

                <div className="mt-4 flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => handleConfirmCashRequest(item.confirmationId, item.loanId)}
                    disabled={processingCashActionId === item.confirmationId}
                    className="rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white disabled:opacity-60"
                  >
                    {processingCashActionId === item.confirmationId ? "Processing..." : "Confirm Cash Payment"}
                  </button>

                  <button
                    type="button"
                    onClick={() => handleRejectCashRequest(item.confirmationId)}
                    disabled={processingCashActionId === item.confirmationId}
                    className="rounded-xl bg-red-600 px-5 py-2.5 text-sm font-medium text-white disabled:opacity-60"
                  >
                    {processingCashActionId === item.confirmationId ? "Processing..." : "Reject Request"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">My Active Loans</h3>
          <button
            type="button"
            onClick={() => loadBorrowerLoans(true)}
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
                <th className="px-3 py-3">Sanctioned</th>
                <th className="px-3 py-3">Disbursed</th>
                <th className="px-3 py-3">Daily EMI</th>
                <th className="px-3 py-3">Remaining</th>
                <th className="px-3 py-3">Overdue</th>
                <th className="px-3 py-3">Next Due</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {activeLoans.length === 0 ? (
                <tr>
                  <td colSpan="9" className="px-3 py-6 text-center text-slate-500">
                    No active loans found.
                  </td>
                </tr>
              ) : (
                activeLoans.map((loan) => (
                  <tr key={loan.loanId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{loan.loanId}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.sanctionedAmount ?? loan.totalAmount}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.disbursedAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.dailyEmi ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.remainingAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{loan.overdueAmount ?? "-"}</td>
                    <td className="px-3 py-3 text-slate-700">{formatDate(loan.nextDueDate)}</td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill text={loan.isClosed ? "CLOSED" : "ACTIVE"} tone={loan.isClosed ? "green" : "blue"} />
                    </td>
                    <td className="px-3 py-3">
                      <button
                        type="button"
                        onClick={() => selectLoan(loan)}
                        className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-medium text-slate-700"
                      >
                        Repay
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
          <div className="rounded-2xl bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-lg font-semibold text-slate-900">Selected Loan Details</h3>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              <SummaryCard
                title="Sanctioned Amount"
                value={selectedLoan.sanctionedAmount ?? selectedLoan.totalAmount ?? "-"}
                subtitle="This is the amount borrower must repay over tenure"
              />
              <SummaryCard
                title="Actually Received"
                value={selectedLoan.disbursedAmount ?? "-"}
                subtitle="Amount credited after upfront deduction"
              />
              <SummaryCard
                title="Daily EMI"
                value={selectedLoan.dailyEmi ?? "-"}
                subtitle="Suggested amount for today’s normal repayment"
              />
              <SummaryCard
                title="Remaining Balance"
                value={selectedLoan.remainingAmount ?? "-"}
                subtitle="Current remaining payable amount"
              />
              <SummaryCard
                title="Overdue Amount"
                value={selectedLoan.overdueAmount ?? "-"}
                subtitle="Unpaid overdue carried forward"
              />
              <SummaryCard
                title="Next Due Date"
                value={formatDate(selectedLoan.nextDueDate)}
                subtitle="Repayment due tracking"
              />
            </div>
          </div>

          <div className="rounded-2xl bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-lg font-semibold text-slate-900">Wallet Repayment</h3>

            <form onSubmit={handleRepayment} className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Amount Paid</label>
                <input
                  type="number"
                  step="0.01"
                  name="amountPaid"
                  value={form.amountPaid}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                  placeholder="Enter repayment amount"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Payment Mode</label>
                <select
                  name="paymentMode"
                  value={form.paymentMode}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                >
                  <option value="WALLET">WALLET</option>
                  <option value="CASH">CASH</option>
                </select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Payment Date (optional)</label>
                <input
                  type="date"
                  name="paymentDate"
                  value={form.paymentDate}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                />
              </div>

              <div className="md:col-span-2 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => ({
                      ...prev,
                      amountPaid: selectedLoan.dailyEmi ? String(selectedLoan.dailyEmi) : "",
                    }))
                  }
                  className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
                >
                  Use Daily EMI
                </button>

                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => ({
                      ...prev,
                      amountPaid: selectedLoan.remainingAmount ? String(selectedLoan.remainingAmount) : "",
                    }))
                  }
                  className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
                >
                  Use Full Remaining
                </button>
              </div>

              <div className="md:col-span-2 flex justify-end">
                <button
                  type="submit"
                  disabled={paying}
                  className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-60"
                >
                  {paying ? "Recording Repayment..." : "Submit Repayment"}
                </button>
              </div>
            </form>
          </div>

          <div className="rounded-2xl bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-lg font-semibold text-slate-900">Repayment History For Selected Loan</h3>

            {loadingRepayments ? (
              <p className="text-sm text-slate-500">Loading repayment history...</p>
            ) : (
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
                      <th className="px-3 py-3">Missed Days</th>
                      <th className="px-3 py-3">Balance</th>
                    </tr>
                  </thead>
                  <tbody>
                    {repayments.length === 0 ? (
                      <tr>
                        <td colSpan="8" className="px-3 py-6 text-center text-slate-500">
                          No repayments recorded for this loan yet.
                        </td>
                      </tr>
                    ) : (
                      repayments.map((item) => (
                        <tr key={item.id} className="border-b border-slate-100">
                          <td className="px-3 py-3 text-slate-700">{item.id}</td>
                          <td className="px-3 py-3 text-slate-700">{formatDate(item.paymentDate)}</td>
                          <td className="px-3 py-3 text-slate-700">{item.amountPaid}</td>
                          <td className="px-3 py-3 text-slate-700">{item.paymentMode}</td>
                          <td className="px-3 py-3 text-slate-700">
                            <StatusPill text={item.paymentStatus || "UNKNOWN"} tone={getRepaymentTone(item.paymentStatus)} />
                          </td>
                          <td className="px-3 py-3 text-slate-700">{item.penaltyAmount ?? 0}</td>
                          <td className="px-3 py-3 text-slate-700">{item.missedDays ?? 0}</td>
                          <td className="px-3 py-3 text-slate-700">{item.balanceAmount ?? "-"}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      ) : null}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">Cash Collection Request History</h3>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Confirmation ID</th>
                <th className="px-3 py-3">Loan ID</th>
                <th className="px-3 py-3">Amount</th>
                <th className="px-3 py-3">Payment Date</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Created At</th>
                <th className="px-3 py-3">Repayment ID</th>
              </tr>
            </thead>
            <tbody>
              {cashRequests.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-3 py-6 text-center text-slate-500">
                    No cash collection requests found.
                  </td>
                </tr>
              ) : (
                cashRequests.map((item) => (
                  <tr key={item.confirmationId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.confirmationId}</td>
                    <td className="px-3 py-3 text-slate-700">{item.loanId}</td>
                    <td className="px-3 py-3 text-slate-700">{item.amount}</td>
                    <td className="px-3 py-3 text-slate-700">{formatDate(item.paymentDate)}</td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill text={item.status} tone={getCashCollectionTone(item.status)} />
                    </td>
                    <td className="px-3 py-3 text-slate-700">{formatDate(item.createdAt)}</td>
                    <td className="px-3 py-3 text-slate-700">{item.repaymentId ?? "-"}</td>
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