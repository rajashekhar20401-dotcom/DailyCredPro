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

function formatDateTime(dateValue) {
  if (!dateValue) return "-";
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return String(dateValue);
  return date.toLocaleString();
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

export default function LenderRepaymentTrackingPage() {
  const { userId } = useAuth();

  const [loans, setLoans] = useState([]);
  const [borrowerMap, setBorrowerMap] = useState({});
  const [repaymentsMap, setRepaymentsMap] = useState({});
  const [cashRequests, setCashRequests] = useState([]);
  const [selectedLoanId, setSelectedLoanId] = useState(null);

  const [profileBorrowerId, setProfileBorrowerId] = useState(null);
  const [selectedBorrowerProfile, setSelectedBorrowerProfile] = useState(null);
  const [selectedBorrowerAnalytics, setSelectedBorrowerAnalytics] = useState(null);
  const [selectedBorrowerLastKnownLocation, setSelectedBorrowerLastKnownLocation] = useState(null);
  const [lastKnownLocationMessage, setLastKnownLocationMessage] = useState("");
  const [loadingProfile, setLoadingProfile] = useState(false);

  const [cashForm, setCashForm] = useState({
    amount: "",
    paymentDate: "",
    lenderNote: "",
  });

  const [loading, setLoading] = useState(true);
  const [initiatingCash, setInitiatingCash] = useState(false);
  const [pageError, setPageError] = useState("");
  const [message, setMessage] = useState("");
  const [latestGeneratedToken, setLatestGeneratedToken] = useState("");

  const selectedLoan = useMemo(() => {
    return loans.find((loan) => loan.loanId === selectedLoanId) || null;
  }, [loans, selectedLoanId]);

  const selectedLoanRepayments = useMemo(() => {
    if (!selectedLoanId) return [];
    return repaymentsMap[selectedLoanId] || [];
  }, [repaymentsMap, selectedLoanId]);

  const totalCollected = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.totalPaidAmount || 0), 0).toFixed(2);
  }, [loans]);

  const totalOutstanding = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.remainingAmount || 0), 0).toFixed(2);
  }, [loans]);

  const totalOverdue = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.overdueAmount || 0), 0).toFixed(2);
  }, [loans]);

  const totalPlatformFee = useMemo(() => {
    return loans.reduce((sum, loan) => sum + Number(loan.platformFeeAmount || 0), 0).toFixed(2);
  }, [loans]);

  const activeLoansCount = useMemo(() => {
    return loans.filter((loan) => loan.isClosed !== true).length;
  }, [loans]);

  const pendingCashRequestsCount = useMemo(() => {
    return cashRequests.filter((item) => item.status === "PENDING_BORROWER_CONFIRMATION").length;
  }, [cashRequests]);

  const flattenedRepayments = useMemo(() => {
    const rows = [];

    Object.entries(repaymentsMap).forEach(([loanId, repayments]) => {
      const numericLoanId = Number(loanId);
      const loan = loans.find((item) => item.loanId === numericLoanId);

      repayments.forEach((repayment) => {
        rows.push({
          ...repayment,
          loanId: numericLoanId,
          borrowerId: loan?.borrowerId,
          borrowerName: borrowerMap[loan?.borrowerId]?.borrowerName || `Borrower ${loan?.borrowerId || "-"}`,
          borrowerPhone: borrowerMap[loan?.borrowerId]?.phoneNumber || "-",
        });
      });
    });

    rows.sort((a, b) => {
      const dateA = new Date(a.paymentDate || 0).getTime();
      const dateB = new Date(b.paymentDate || 0).getTime();
      return dateB - dateA;
    });

    return rows;
  }, [repaymentsMap, loans, borrowerMap]);

  const todayCollection = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10);

    return flattenedRepayments
      .filter((item) => item.paymentDate && String(item.paymentDate).slice(0, 10) === today)
      .reduce((sum, item) => sum + Number(item.amountPaid || 0), 0)
      .toFixed(2);
  }, [flattenedRepayments]);

  const todayPaidCount = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10);

    return flattenedRepayments.filter(
      (item) =>
        item.paymentDate &&
        String(item.paymentDate).slice(0, 10) === today &&
        Number(item.amountPaid || 0) > 0
    ).length;
  }, [flattenedRepayments]);

  async function loadPage(showMessage = false) {
    if (!userId) return;

    setLoading(true);
    setPageError("");

    try {
      const [loansResponse, cashResponse] = await Promise.all([
        api.get(`/api/loans/lender/${userId}`),
        api.get(`/api/cash-collections/lender/${userId}`),
      ]);

      const lenderLoans = loansResponse.data?.data || [];
      setLoans(lenderLoans);
      setCashRequests(cashResponse.data?.data || []);

      const borrowerIds = [...new Set(lenderLoans.map((loan) => loan.borrowerId).filter(Boolean))];

      const borrowerResults = await Promise.all(
        borrowerIds.map(async (borrowerId) => {
          try {
            const response = await api.get(`/api/borrowers/${borrowerId}`);
            return [borrowerId, response.data?.data || null];
          } catch {
            return [borrowerId, null];
          }
        })
      );

      setBorrowerMap(Object.fromEntries(borrowerResults));

      const repaymentsEntries = await Promise.all(
        lenderLoans.map(async (loan) => {
          try {
            const response = await api.get(`/api/repayments/loan/${loan.loanId}`);
            return [loan.loanId, response.data || []];
          } catch {
            return [loan.loanId, []];
          }
        })
      );

      const repaymentsObj = Object.fromEntries(repaymentsEntries);
      setRepaymentsMap(repaymentsObj);

      if (!selectedLoanId && lenderLoans.length > 0) {
        setSelectedLoanId(lenderLoans[0].loanId);
      }

      if (showMessage) {
        setMessage("Lender repayment data refreshed successfully.");
      }
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load lender repayment tracking data.");
    } finally {
      setLoading(false);
    }
  }

  async function openBorrowerProfile(borrowerId) {
    if (!borrowerId) return;

    setProfileBorrowerId(borrowerId);
    setLoadingProfile(true);
    setSelectedBorrowerProfile(null);
    setSelectedBorrowerAnalytics(null);
    setSelectedBorrowerLastKnownLocation(null);
    setLastKnownLocationMessage("");
    setPageError("");
    setMessage("");

    try {
      const [profileResponse, analyticsResponse] = await Promise.all([
        api.get(`/api/borrowers/${borrowerId}`),
        api.get(`/api/borrower-analytics/${borrowerId}/summary`),
      ]);

      setSelectedBorrowerProfile(profileResponse.data?.data || null);
      setSelectedBorrowerAnalytics(analyticsResponse.data?.data || null);

      try {
        const locationResponse = await api.get(
          `/api/location/lenders/${userId}/borrowers/${borrowerId}/last-known-location`
        );
        setSelectedBorrowerLastKnownLocation(locationResponse.data?.data || null);
      } catch (locationErr) {
        setSelectedBorrowerLastKnownLocation(null);
        setLastKnownLocationMessage(
          locationErr.response?.data?.message ||
            "Last known location is not available or borrower is not yet eligible for location access."
        );
      }
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load borrower profile.");
    } finally {
      setLoadingProfile(false);
    }
  }

  async function handleInitiateCashCollection(event) {
    event.preventDefault();

    if (!selectedLoan) {
      setPageError("Please select a loan first.");
      return;
    }

    setInitiatingCash(true);
    setPageError("");
    setMessage("");
    setLatestGeneratedToken("");

    try {
      const payload = {
        loanId: selectedLoan.loanId,
        amount: cashForm.amount === "" ? null : Number(cashForm.amount),
        paymentDate: cashForm.paymentDate || null,
        lenderNote: cashForm.lenderNote || null,
      };

      const response = await api.post(`/api/cash-collections/lender/${userId}/initiate`, payload);
      const confirmation = response.data?.data;

      setMessage(response.data?.message || "Cash collection initiated successfully.");
      setLatestGeneratedToken(confirmation?.generatedToken || "");

      setCashForm({
        amount: selectedLoan.dailyEmi ? String(selectedLoan.dailyEmi) : "",
        paymentDate: "",
        lenderNote: "",
      });

      await loadPage(false);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to initiate cash collection.");
    } finally {
      setInitiatingCash(false);
    }
  }

  useEffect(() => {
    loadPage(false);
  }, [userId]);

  useEffect(() => {
    if (selectedLoan) {
      setCashForm({
        amount: selectedLoan.dailyEmi ? String(selectedLoan.dailyEmi) : "",
        paymentDate: "",
        lenderNote: "",
      });
    }
  }, [selectedLoan]);

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading lender repayment tracking...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Lender Repayment Tracking</h2>
        <p className="mt-2 text-sm text-slate-500">
          Track active loans, collections, borrower repayment behavior, overdue amounts, and cash confirmations.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3 xl:grid-cols-6">
        <SummaryCard title="Total Loans" value={loans.length} subtitle="All lender-side loans" />
        <SummaryCard title="Active Loans" value={activeLoansCount} subtitle="Still open" />
        <SummaryCard title="Total Collected" value={totalCollected} subtitle="All repayments received" />
        <SummaryCard title="Outstanding" value={totalOutstanding} subtitle="Remaining loan balance" />
        <SummaryCard title="Overdue" value={totalOverdue} subtitle="Current overdue exposure" />
        <SummaryCard title="Today Collection" value={todayCollection} subtitle={`${todayPaidCount} repayments today`} />
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard title="Platform Fee Accrued" value={totalPlatformFee} subtitle="Calculated from lender profit" />
        <SummaryCard
          title="Tracked Repayment Events"
          value={flattenedRepayments.length}
          subtitle="Across all loans of this lender"
        />
        <SummaryCard
          title="Pending Cash Confirmations"
          value={pendingCashRequestsCount}
          subtitle="Awaiting borrower token confirmation"
        />
      </div>

      {(message || pageError || latestGeneratedToken) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          {message ? (
            <div className="mb-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {message}
            </div>
          ) : null}

          {latestGeneratedToken ? (
            <div className="mb-3 rounded-xl bg-blue-50 px-4 py-3 text-sm text-blue-800">
              Share this confirmation token with the borrower: <span className="font-bold">{latestGeneratedToken}</span>
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
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Lender Loans</h3>
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
                <th className="px-3 py-3">Borrower</th>
                <th className="px-3 py-3">Phone</th>
                <th className="px-3 py-3">Sanctioned</th>
                <th className="px-3 py-3">Disbursed</th>
                <th className="px-3 py-3">Collected</th>
                <th className="px-3 py-3">Remaining</th>
                <th className="px-3 py-3">Overdue</th>
                <th className="px-3 py-3">Next Due</th>
                <th className="px-3 py-3">Loan Status</th>
                <th className="px-3 py-3">Latest Repayment</th>
                <th className="px-3 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {loans.length === 0 ? (
                <tr>
                  <td colSpan="12" className="px-3 py-6 text-center text-slate-500">
                    No lender loans found.
                  </td>
                </tr>
              ) : (
                loans.map((loan) => {
                  const borrower = borrowerMap[loan.borrowerId];
                  const repaymentHistory = repaymentsMap[loan.loanId] || [];
                  const latestRepayment = repaymentHistory.length > 0 ? repaymentHistory[repaymentHistory.length - 1] : null;

                  return (
                    <tr key={loan.loanId} className="border-b border-slate-100">
                      <td className="px-3 py-3 text-slate-700">{loan.loanId}</td>
                      <td className="px-3 py-3 text-slate-700">{borrower?.borrowerName || `Borrower ${loan.borrowerId}`}</td>
                      <td className="px-3 py-3 text-slate-700">{borrower?.phoneNumber || "-"}</td>
                      <td className="px-3 py-3 text-slate-700">{loan.sanctionedAmount ?? loan.totalAmount ?? "-"}</td>
                      <td className="px-3 py-3 text-slate-700">{loan.disbursedAmount ?? "-"}</td>
                      <td className="px-3 py-3 text-slate-700">{loan.totalPaidAmount ?? 0}</td>
                      <td className="px-3 py-3 text-slate-700">{loan.remainingAmount ?? "-"}</td>
                      <td className="px-3 py-3 text-slate-700">{loan.overdueAmount ?? 0}</td>
                      <td className="px-3 py-3 text-slate-700">{formatDate(loan.nextDueDate)}</td>
                      <td className="px-3 py-3 text-slate-700">
                        <StatusPill text={loan.isClosed ? "CLOSED" : "ACTIVE"} tone={loan.isClosed ? "green" : "blue"} />
                      </td>
                      <td className="px-3 py-3 text-slate-700">
                        {latestRepayment ? (
                          <StatusPill
                            text={latestRepayment.paymentStatus || "UNKNOWN"}
                            tone={getRepaymentTone(latestRepayment.paymentStatus)}
                          />
                        ) : (
                          "-"
                        )}
                      </td>
                      <td className="px-3 py-3">
                        <div className="flex flex-wrap gap-2">
                          <button
                            type="button"
                            onClick={() => setSelectedLoanId(loan.loanId)}
                            className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-medium text-slate-700"
                          >
                            Track Loan
                          </button>
                          <button
                            type="button"
                            onClick={() => openBorrowerProfile(loan.borrowerId)}
                            className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-medium text-slate-700"
                          >
                            Borrower Profile
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {profileBorrowerId ? (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-slate-900">Borrower Profile Panel</h3>
            <button
              type="button"
              onClick={() => {
                setProfileBorrowerId(null);
                setSelectedBorrowerProfile(null);
                setSelectedBorrowerAnalytics(null);
                setSelectedBorrowerLastKnownLocation(null);
                setLastKnownLocationMessage("");
              }}
              className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Close Profile
            </button>
          </div>

          {loadingProfile ? (
            <p className="text-sm text-slate-500">Loading borrower profile...</p>
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <SummaryCard
                  title="Borrower Name"
                  value={selectedBorrowerProfile?.borrowerName || "-"}
                  subtitle={selectedBorrowerProfile?.phoneNumber || "-"}
                />
                <SummaryCard
                  title="Internal Score"
                  value={selectedBorrowerAnalytics?.internalCreditScore ?? selectedBorrowerProfile?.internalCreditScore ?? 0}
                  subtitle="Internal generated credit score"
                />
                <SummaryCard
                  title="Risk Category"
                  value={selectedBorrowerAnalytics?.riskCategory || selectedBorrowerProfile?.riskCategory || "UNKNOWN"}
                  subtitle={`Risk score: ${selectedBorrowerAnalytics?.riskScore ?? selectedBorrowerProfile?.riskScore ?? 0}`}
                />
                <SummaryCard
                  title="Eligibility Status"
                  value={selectedBorrowerAnalytics?.eligibilityStatus || selectedBorrowerProfile?.eligibilityStatus || "-"}
                  subtitle={selectedBorrowerAnalytics?.eligibilityTier || selectedBorrowerProfile?.eligibilityTier || "-"}
                />
                <SummaryCard
                  title="Max Eligible Amount"
                  value={selectedBorrowerAnalytics?.maxEligibleLoanAmount ?? selectedBorrowerProfile?.maxEligibleLoanAmount ?? 0}
                  subtitle="Platform-side eligibility ceiling"
                />
                <SummaryCard
                  title="Outstanding Amount"
                  value={selectedBorrowerAnalytics?.currentOutstandingAmount ?? selectedBorrowerProfile?.currentOutstandingAmount ?? 0}
                  subtitle={`Active loans: ${selectedBorrowerAnalytics?.activeLoans ?? selectedBorrowerProfile?.activeLoanCount ?? 0}`}
                />
              </div>

              <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
                <div className="rounded-2xl bg-slate-50 p-5">
                  <h4 className="mb-3 text-base font-semibold text-slate-900">Contact & Flags</h4>
                  <div className="space-y-2 text-sm text-slate-700">
                    <p><span className="font-medium">Email:</span> {selectedBorrowerProfile?.email || "-"}</p>
                    <p><span className="font-medium">Phone:</span> {selectedBorrowerProfile?.phoneNumber || "-"}</p>
                    <p><span className="font-medium">Pincode:</span> {selectedBorrowerProfile?.pincode || "-"}</p>
                    <p><span className="font-medium">Fraud Flag:</span> {String(selectedBorrowerProfile?.fraudFlag ?? false)}</p>
                    <p><span className="font-medium">Blacklisted:</span> {String(selectedBorrowerProfile?.blacklisted ?? false)}</p>
                    <p><span className="font-medium">Manual Review:</span> {String(selectedBorrowerProfile?.manualReviewFlag ?? false)}</p>
                  </div>
                </div>

                <div className="rounded-2xl bg-slate-50 p-5">
                  <h4 className="mb-3 text-base font-semibold text-slate-900">Repayment Behavior</h4>
                  <div className="space-y-2 text-sm text-slate-700">
                    <p><span className="font-medium">Total Loans Taken:</span> {selectedBorrowerAnalytics?.totalLoansTaken ?? selectedBorrowerProfile?.totalLoansTaken ?? 0}</p>
                    <p><span className="font-medium">Defaulted Loan Count:</span> {selectedBorrowerAnalytics?.defaultedLoanCount ?? selectedBorrowerProfile?.defaultedLoanCount ?? 0}</p>
                    <p><span className="font-medium">Total Missed Days:</span> {selectedBorrowerAnalytics?.totalMissedDays ?? selectedBorrowerProfile?.totalMissedDays ?? 0}</p>
                    <p><span className="font-medium">Total Partial Payments:</span> {selectedBorrowerAnalytics?.totalPartialPayments ?? selectedBorrowerProfile?.totalPartialDays ?? 0}</p>
                    <p><span className="font-medium">Total Advance Payments:</span> {selectedBorrowerAnalytics?.totalAdvancePayments ?? selectedBorrowerProfile?.totalAdvanceDays ?? 0}</p>
                    <p><span className="font-medium">Max Consecutive Missed Days:</span> {selectedBorrowerAnalytics?.maxConsecutiveMissedDays ?? selectedBorrowerProfile?.maxConsecutiveMissedDays ?? 0}</p>
                  </div>
                </div>
              </div>

              <div className="mt-6 rounded-2xl bg-slate-50 p-5">
                <h4 className="mb-3 text-base font-semibold text-slate-900">Last Known Location</h4>

                {selectedBorrowerLastKnownLocation ? (
                  <div className="space-y-2 text-sm text-slate-700">
                    <p><span className="font-medium">Latitude:</span> {selectedBorrowerLastKnownLocation.latitude}</p>
                    <p><span className="font-medium">Longitude:</span> {selectedBorrowerLastKnownLocation.longitude}</p>
                    <p><span className="font-medium">Last Updated:</span> {formatDateTime(selectedBorrowerLastKnownLocation.lastUpdatedAt)}</p>
                    <p><span className="font-medium">Access Reason:</span> {selectedBorrowerLastKnownLocation.accessReason || "-"}</p>
                  </div>
                ) : (
                  <div className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                    {lastKnownLocationMessage ||
                      "Last known location will become available only after this borrower crosses 10 consecutive missed payment days and has a saved location record."}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      ) : null}

      {selectedLoan ? (
        <>
          <div className="rounded-2xl bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-lg font-semibold text-slate-900">Selected Loan Detail</h3>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              <SummaryCard
                title="Borrower"
                value={borrowerMap[selectedLoan.borrowerId]?.borrowerName || selectedLoan.borrowerId}
                subtitle={borrowerMap[selectedLoan.borrowerId]?.phoneNumber || "-"}
              />
              <SummaryCard
                title="Daily EMI"
                value={selectedLoan.dailyEmi ?? "-"}
                subtitle="Normal due amount per repayment day"
              />
              <SummaryCard
                title="Remaining"
                value={selectedLoan.remainingAmount ?? "-"}
                subtitle="Current payable balance"
              />
              <SummaryCard
                title="Overdue"
                value={selectedLoan.overdueAmount ?? 0}
                subtitle="Overdue carried forward"
              />
              <SummaryCard
                title="Penalty"
                value={selectedLoan.penaltyAmount ?? 0}
                subtitle="Current penalty field on loan"
              />
              <SummaryCard
                title="Platform Fee"
                value={selectedLoan.platformFeeAmount ?? 0}
                subtitle="Accrued platform fee for this loan"
              />
            </div>
          </div>

          <div className="rounded-2xl bg-white p-6 shadow-sm">
            <h3 className="mb-4 text-lg font-semibold text-slate-900">Initiate Cash Collection Confirmation</h3>

            <form onSubmit={handleInitiateCashCollection} className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Cash Amount Collected</label>
                <input
                  type="number"
                  step="0.01"
                  name="amount"
                  value={cashForm.amount}
                  onChange={(e) => setCashForm((prev) => ({ ...prev, amount: e.target.value }))}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                  placeholder="Enter amount collected in cash"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Payment Date</label>
                <input
                  type="date"
                  name="paymentDate"
                  value={cashForm.paymentDate}
                  onChange={(e) => setCashForm((prev) => ({ ...prev, paymentDate: e.target.value }))}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                />
              </div>

              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-slate-700">Lender Note</label>
                <input
                  name="lenderNote"
                  value={cashForm.lenderNote}
                  onChange={(e) => setCashForm((prev) => ({ ...prev, lenderNote: e.target.value }))}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                  placeholder="Optional note for borrower/admin reference"
                />
              </div>

              <div className="md:col-span-2 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() =>
                    setCashForm((prev) => ({
                      ...prev,
                      amount: selectedLoan.dailyEmi ? String(selectedLoan.dailyEmi) : "",
                    }))
                  }
                  className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
                >
                  Use Daily EMI
                </button>

                <button
                  type="button"
                  onClick={() =>
                    setCashForm((prev) => ({
                      ...prev,
                      amount: selectedLoan.remainingAmount ? String(selectedLoan.remainingAmount) : "",
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
                  disabled={initiatingCash}
                  className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-60"
                >
                  {initiatingCash ? "Initiating..." : "Initiate Cash Collection"}
                </button>
              </div>
            </form>
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
                    <th className="px-3 py-3">Missed Days</th>
                    <th className="px-3 py-3">Balance</th>
                    <th className="px-3 py-3">Reference</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedLoanRepayments.length === 0 ? (
                    <tr>
                      <td colSpan="9" className="px-3 py-6 text-center text-slate-500">
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
        <h3 className="mb-4 text-lg font-semibold text-slate-900">Cash Collection Requests</h3>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Confirmation ID</th>
                <th className="px-3 py-3">Loan ID</th>
                <th className="px-3 py-3">Borrower</th>
                <th className="px-3 py-3">Amount</th>
                <th className="px-3 py-3">Payment Date</th>
                <th className="px-3 py-3">Token</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Expires</th>
              </tr>
            </thead>
            <tbody>
              {cashRequests.length === 0 ? (
                <tr>
                  <td colSpan="8" className="px-3 py-6 text-center text-slate-500">
                    No cash collection requests created yet.
                  </td>
                </tr>
              ) : (
                cashRequests.map((item) => (
                  <tr key={item.confirmationId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.confirmationId}</td>
                    <td className="px-3 py-3 text-slate-700">{item.loanId}</td>
                    <td className="px-3 py-3 text-slate-700">{borrowerMap[item.borrowerId]?.borrowerName || item.borrowerId}</td>
                    <td className="px-3 py-3 text-slate-700">{item.amount}</td>
                    <td className="px-3 py-3 text-slate-700">{formatDate(item.paymentDate)}</td>
                    <td className="px-3 py-3 text-slate-700">
                      {item.status === "PENDING_BORROWER_CONFIRMATION" ? item.generatedToken : "-"}
                    </td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill text={item.status} tone={getCashCollectionTone(item.status)} />
                    </td>
                    <td className="px-3 py-3 text-slate-700">{formatDateTime(item.expiresAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">Recent Repayment Events</h3>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Loan ID</th>
                <th className="px-3 py-3">Borrower</th>
                <th className="px-3 py-3">Phone</th>
                <th className="px-3 py-3">Date</th>
                <th className="px-3 py-3">Amount</th>
                <th className="px-3 py-3">Mode</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Balance</th>
              </tr>
            </thead>
            <tbody>
              {flattenedRepayments.length === 0 ? (
                <tr>
                  <td colSpan="8" className="px-3 py-6 text-center text-slate-500">
                    No repayment events found yet.
                  </td>
                </tr>
              ) : (
                flattenedRepayments.slice(0, 50).map((item) => (
                  <tr key={`${item.loanId}-${item.id}`} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.loanId}</td>
                    <td className="px-3 py-3 text-slate-700">{item.borrowerName}</td>
                    <td className="px-3 py-3 text-slate-700">{item.borrowerPhone}</td>
                    <td className="px-3 py-3 text-slate-700">{formatDateTime(item.paymentDate)}</td>
                    <td className="px-3 py-3 text-slate-700">{item.amountPaid}</td>
                    <td className="px-3 py-3 text-slate-700">{item.paymentMode}</td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill text={item.paymentStatus || "UNKNOWN"} tone={getRepaymentTone(item.paymentStatus)} />
                    </td>
                    <td className="px-3 py-3 text-slate-700">{item.balanceAmount ?? "-"}</td>
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