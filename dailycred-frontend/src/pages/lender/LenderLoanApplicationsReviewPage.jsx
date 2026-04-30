import { useEffect, useMemo, useState } from "react";
import api from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

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

function getRepaymentSignal(analytics) {
  if (!analytics) return "No analytics available yet.";
  if (analytics.riskCategory === "LOW") return "High chance of faster repayment.";
  if (analytics.riskCategory === "MEDIUM") return "Moderate repayment risk.";
  if (analytics.riskCategory === "HIGH") return "High chance of delayed repayments.";
  return analytics.recommendation || "No recommendation available.";
}

export default function LenderLoanApplicationsReviewPage() {
  const { userId } = useAuth();

  const [applications, setApplications] = useState([]);
  const [plans, setPlans] = useState([]);
  const [borrowerMap, setBorrowerMap] = useState({});
  const [analyticsMap, setAnalyticsMap] = useState({});
  const [remarksMap, setRemarksMap] = useState({});
  const [filterStatus, setFilterStatus] = useState("ALL");

  const [loading, setLoading] = useState(true);
  const [decisionLoadingId, setDecisionLoadingId] = useState(null);

  const [message, setMessage] = useState("");
  const [pageError, setPageError] = useState("");

  const planNameMap = useMemo(() => {
    const map = {};
    plans.forEach((plan) => {
      map[plan.id] = plan.planName;
    });
    return map;
  }, [plans]);

  const filteredApplications = useMemo(() => {
    if (filterStatus === "ALL") return applications;
    return applications.filter((item) => item.applicationStatus === filterStatus);
  }, [applications, filterStatus]);

  async function loadApplications() {
    if (!userId) return;

    setLoading(true);
    setPageError("");

    try {
      const [applicationsResponse, plansResponse] = await Promise.all([
        api.get(`/api/loan-application/lenderApplications/lenderId/${userId}`),
        api.get(`/loan-plans/lender/${userId}`),
      ]);

      const applicationList = applicationsResponse.data?.data || [];
      const planList = plansResponse.data?.data || [];

      setApplications(applicationList);
      setPlans(planList);

      const borrowerIds = [...new Set(applicationList.map((item) => item.borrowerId))];

      const borrowerEntries = await Promise.all(
        borrowerIds.map(async (borrowerId) => {
          try {
            const response = await api.get(`/api/borrowers/${borrowerId}`);
            return [borrowerId, response.data?.data || null];
          } catch {
            return [borrowerId, null];
          }
        })
      );

      const analyticsEntries = await Promise.all(
        borrowerIds.map(async (borrowerId) => {
          try {
            const response = await api.get(`/api/borrower-analytics/${borrowerId}/summary`);
            return [borrowerId, response.data?.data || null];
          } catch {
            return [borrowerId, null];
          }
        })
      );

      setBorrowerMap(Object.fromEntries(borrowerEntries));
      setAnalyticsMap(Object.fromEntries(analyticsEntries));
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load lender applications.");
    } finally {
      setLoading(false);
    }
  }

  async function handleDecision(applicationId, actionStatus) {
    setDecisionLoadingId(applicationId);
    setPageError("");
    setMessage("");

    try {
      const response = await api.patch(
        `/api/loan-application/${applicationId}/decision`,
        {
          applicationStatus: actionStatus,
          remarks: remarksMap[applicationId] || "",
        },
        {
          params: { lenderId: userId },
        }
      );

      setMessage(response.data?.message || "Loan application decision updated successfully.");
      await loadApplications();
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to update loan application decision.");
    } finally {
      setDecisionLoadingId(null);
    }
  }

  useEffect(() => {
    loadApplications();
  }, [userId]);

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading lender application review center...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Review Borrower Applications</h2>
        <p className="mt-2 text-sm text-slate-500">
          Review all applications sent to this lender, see borrower contact and analytics, then approve or reject.
        </p>
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
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-lg font-semibold text-slate-900">Applications</h3>

          <div className="flex items-center gap-3">
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="ALL">All</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
              <option value="CANCELLED">Cancelled</option>
            </select>

            <button
              type="button"
              onClick={loadApplications}
              className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Refresh
            </button>
          </div>
        </div>

        <div className="space-y-4">
          {filteredApplications.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500">
              No applications found for the selected filter.
            </div>
          ) : (
            filteredApplications.map((application) => {
              const borrower = borrowerMap[application.borrowerId];
              const analytics = analyticsMap[application.borrowerId];

              return (
                <div key={application.applicationId} className="rounded-2xl border border-slate-200 p-5">
                  <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <h4 className="text-lg font-semibold text-slate-900">
                        Application #{application.applicationId}
                      </h4>
                      <p className="text-sm text-slate-500">
                        Plan: {planNameMap[application.planId] || application.planId}
                      </p>
                    </div>

                    <StatusPill
                      text={application.applicationStatus || "UNKNOWN"}
                      tone={
                        application.applicationStatus === "APPROVED"
                          ? "green"
                          : application.applicationStatus === "REJECTED"
                          ? "red"
                          : application.applicationStatus === "PENDING"
                          ? "yellow"
                          : "slate"
                      }
                    />
                  </div>

                  <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                    <div className="rounded-2xl bg-slate-50 p-4">
                      <p className="mb-2 text-sm font-semibold text-slate-800">Borrower Contact</p>
                      <p className="text-sm text-slate-700">{borrower?.borrowerName || "Unknown Borrower"}</p>
                      <p className="text-sm text-slate-700">{borrower?.phoneNumber || "-"}</p>
                      <p className="text-sm text-slate-700">{borrower?.email || "-"}</p>
                    </div>

                    <div className="rounded-2xl bg-slate-50 p-4">
                      <p className="mb-2 text-sm font-semibold text-slate-800">Application Details</p>
                      <p className="text-sm text-slate-700">Loan Amount: {application.loanAmount}</p>
                      <p className="text-sm text-slate-700">Age: {application.age}</p>
                      <p className="text-sm text-slate-700">Income: {application.monthlyIncome}</p>
                      <p className="text-sm text-slate-700">Employee Type: {application.employeeType}</p>
                      <p className="text-sm text-slate-700">Pin Code: {application.pinCode}</p>
                      <p className="text-sm text-slate-700">Applied At: {application.appliedAt ? new Date(application.appliedAt).toLocaleString() : "-"}</p>
                    </div>

                    <div className="rounded-2xl bg-slate-50 p-4">
                      <p className="mb-2 text-sm font-semibold text-slate-800">Borrower Analytics</p>
                      <p className="text-sm text-slate-700">Internal Score: {analytics?.internalCreditScore ?? "-"}</p>
                      <p className="text-sm text-slate-700">Risk Score: {analytics?.riskScore ?? "-"}</p>
                      <p className="text-sm text-slate-700">Risk Category: {analytics?.riskCategory ?? "-"}</p>
                      <p className="text-sm text-slate-700">Active Loans: {analytics?.activeLoans ?? "-"}</p>
                      <p className="mt-2 text-sm font-medium text-slate-800">
                        {getRepaymentSignal(analytics)}
                      </p>
                    </div>
                  </div>

                  <div className="mt-4">
                    <label className="mb-1 block text-sm font-medium text-slate-700">Remarks</label>
                    <textarea
                      value={remarksMap[application.applicationId] || ""}
                      onChange={(e) =>
                        setRemarksMap((prev) => ({
                          ...prev,
                          [application.applicationId]: e.target.value,
                        }))
                      }
                      rows="3"
                      className="w-full rounded-xl border border-slate-300 px-3 py-2"
                      placeholder="Type approval/rejection remarks"
                    />
                  </div>

                  {application.applicationStatus === "PENDING" ? (
                    <div className="mt-4 flex flex-wrap gap-3">
                      <button
                        type="button"
                        onClick={() => handleDecision(application.applicationId, "APPROVED")}
                        disabled={decisionLoadingId === application.applicationId}
                        className="rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white disabled:opacity-60"
                      >
                        {decisionLoadingId === application.applicationId ? "Processing..." : "Approve"}
                      </button>

                      <button
                        type="button"
                        onClick={() => handleDecision(application.applicationId, "REJECTED")}
                        disabled={decisionLoadingId === application.applicationId}
                        className="rounded-xl bg-red-600 px-5 py-2.5 text-sm font-medium text-white disabled:opacity-60"
                      >
                        {decisionLoadingId === application.applicationId ? "Processing..." : "Reject"}
                      </button>
                    </div>
                  ) : null}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}