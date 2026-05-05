import { useState } from "react";
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

export default function ReportsPage() {
  const { role, userId } = useAuth();

  const [loadingBorrowerPdf, setLoadingBorrowerPdf] = useState(false);
  const [loadingLenderPdf, setLoadingLenderPdf] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function downloadBorrowerReport() {
    if (!userId) return;

    setLoadingBorrowerPdf(true);
    setMessage("");
    setError("");

    try {
      const response = await api.get(`/api/reports/borrowers/${userId}/analytics.pdf`, {
        responseType: "blob",
      });

      const blob = new Blob([response.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);

      const link = document.createElement("a");
      link.href = url;
      link.download = `borrower-analytics-${userId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(url);
      setMessage("Borrower analytics PDF downloaded successfully.");
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Failed to download borrower report.");
    } finally {
      setLoadingBorrowerPdf(false);
    }
  }

  async function downloadLenderReport() {
    if (!userId) return;

    setLoadingLenderPdf(true);
    setMessage("");
    setError("");

    try {
      const response = await api.get(`/api/reports/lenders/${userId}/dashboard.pdf`, {
        responseType: "blob",
      });

      const blob = new Blob([response.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);

      const link = document.createElement("a");
      link.href = url;
      link.download = `lender-dashboard-${userId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(url);
      setMessage("Lender dashboard PDF downloaded successfully.");
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Failed to download lender report.");
    } finally {
      setLoadingLenderPdf(false);
    }
  }

  if (role === "ADMIN") {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Reports Center</h2>
        <p className="mt-2 text-slate-600">
          Report downloads are currently available for borrower and lender roles. Admin platform reports will be added later.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Reports Center</h2>
        <p className="mt-2 text-sm text-slate-500">
          Download your role-specific PDF reports directly from the backend.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard
          title="Role"
          value={role || "-"}
          subtitle="Current logged-in account type"
        />
        <SummaryCard
          title="User ID"
          value={userId || "-"}
          subtitle="Current authenticated user"
        />
        <SummaryCard
          title="Available Report"
          value={role === "BORROWER" ? "BORROWER PDF" : "LENDER PDF"}
          subtitle="Only your role-specific report is shown"
        />
      </div>

      {(message || error) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          {message ? (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {message}
            </div>
          ) : null}

          {error ? (
            <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
              {error}
            </div>
          ) : null}
        </div>
      )}

      {role === "BORROWER" && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Borrower Analytics PDF</h3>
          <p className="mb-5 text-sm text-slate-600">
            This report includes borrower analytics such as credit score, risk score, eligibility status, missed days, late payments, outstanding amount, and repayment history.
          </p>

          <button
            type="button"
            onClick={downloadBorrowerReport}
            disabled={loadingBorrowerPdf}
            className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-50"
          >
            {loadingBorrowerPdf ? "Downloading..." : "Download Borrower Report"}
          </button>
        </div>
      )}

      {role === "LENDER" && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Lender Dashboard PDF</h3>
          <p className="mb-5 text-sm text-slate-600">
            This report includes lender analytics such as total disbursed amount, total collected, projected profit, outstanding amount, penalty collected, and dashboard summary.
          </p>

          <button
            type="button"
            onClick={downloadLenderReport}
            disabled={loadingLenderPdf}
            className="rounded-xl border border-slate-300 bg-white px-6 py-3 font-medium text-slate-700 disabled:opacity-50"
          >
            {loadingLenderPdf ? "Downloading..." : "Download Lender Report"}
          </button>
        </div>
      )}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-3 text-lg font-semibold text-slate-900">How this page works</h3>
        <div className="space-y-2 text-sm text-slate-600">
          <p>1. It checks the logged-in role from frontend auth state.</p>
          <p>2. It shows only the report that belongs to that role.</p>
          <p>3. It calls the protected PDF endpoint using axios with JWT header attached automatically.</p>
          <p>4. It receives the PDF as a blob and creates a temporary browser download link.</p>
        </div>
      </div>
    </div>
  );
}