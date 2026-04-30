import { useEffect, useMemo, useState } from "react";
import api from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

const initialForm = {
  planName: "",
  amount: "",
  interestPerDay: "",
  planDuration: "",
};

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

function calculatePreview(amount, percent, tenureDays) {
  const sanctionedAmount = Number(amount || 0);
  const upfrontPercent = Number(percent || 0);
  const days = Number(tenureDays || 0);

  if (!sanctionedAmount || !upfrontPercent || !days) {
    return null;
  }

  const upfrontInterest = Number(((sanctionedAmount * upfrontPercent) / 100).toFixed(2));
  const disbursedAmount = Number((sanctionedAmount - upfrontInterest).toFixed(2));
  const totalRepayableAmount = sanctionedAmount;
  const dailyEmi = Number((sanctionedAmount / days).toFixed(2));
  const lenderGrossProfit = upfrontInterest;
  const estimatedPlatformFee = Number((lenderGrossProfit * 0.01).toFixed(2));

  return {
    upfrontInterest,
    disbursedAmount,
    totalRepayableAmount,
    dailyEmi,
    lenderGrossProfit,
    estimatedPlatformFee,
  };
}

function derivePlanDefaults(amount, lenderPincode) {
  const principal = Number(amount || 0);

  let minMonthlyIncome = 10000;

  if (principal > 100000 && principal <= 200000) {
    minMonthlyIncome = 30000;
  } else if (principal > 200000 && principal <= 300000) {
    minMonthlyIncome = 50000;
  } else if (principal > 300000) {
    minMonthlyIncome = 75000;
  }

  return {
    penaltyAmount: 0,
    minCibil: 0,
    minAge: 18,
    maxAge: 70,
    minMonthlyIncome,
    servicePinCode: lenderPincode || "",
    maxActiveLoans: 999,
    employeeType: null,
  };
}

export default function LenderLoanPlansPage() {
  const { userId } = useAuth();

  const [plans, setPlans] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editingPlanId, setEditingPlanId] = useState(null);
  const [lenderPincode, setLenderPincode] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [pageError, setPageError] = useState("");
  const [message, setMessage] = useState("");

  const activePlansCount = useMemo(() => {
    return plans.filter((item) => item.status === "ACTIVE").length;
  }, [plans]);

  const averagePlanAmount = useMemo(() => {
    if (plans.length === 0) return 0;
    const total = plans.reduce((sum, item) => sum + (item.amount || 0), 0);
    return Math.round((total / plans.length) * 100) / 100;
  }, [plans]);

  const preview = useMemo(() => {
    return calculatePreview(form.amount, form.interestPerDay, form.planDuration);
  }, [form]);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function resetForm() {
    setForm(initialForm);
    setEditingPlanId(null);
    setMessage("");
    setPageError("");
  }

  function startEdit(plan) {
    setEditingPlanId(plan.id);
    setForm({
      planName: plan.planName ?? "",
      amount: plan.amount ?? "",
      interestPerDay: plan.interestPerDay ?? "",
      planDuration: plan.planDuration ?? "",
    });
    setMessage("Editing selected plan.");
    setPageError("");
  }

  async function loadPlans(showMessage = false) {
    if (!userId) return;

    setLoading(true);
    setPageError("");

    try {
      const [plansResponse, lenderResponse] = await Promise.all([
        api.get(`/loan-plans/lender/${userId}`),
        api.get(`/api/lenders/${userId}`),
      ]);

      setPlans(plansResponse.data?.data || []);
      setLenderPincode(lenderResponse.data?.data?.pincode || "");

      if (showMessage) {
        setMessage("Loan plans refreshed successfully.");
      }
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load loan plans.");
    } finally {
      setLoading(false);
    }
  }

  function buildPayload() {
    const defaults = derivePlanDefaults(form.amount, lenderPincode);

    return {
      planName: form.planName,
      amount: Number(form.amount),
      interestPerDay: Number(form.interestPerDay),
      planDuration: Number(form.planDuration),

      penaltyAmount: defaults.penaltyAmount,
      minCibil: defaults.minCibil,
      minAge: defaults.minAge,
      maxAge: defaults.maxAge,
      minMonthlyIncome: defaults.minMonthlyIncome,
      servicePinCode: defaults.servicePinCode,
      maxActiveLoans: defaults.maxActiveLoans,
      employeeType: defaults.employeeType,
    };
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setPageError("");
    setMessage("");

    try {
      if (!form.planName || !form.amount || !form.interestPerDay || !form.planDuration) {
        throw new Error("Please fill all required loan plan fields.");
      }

      if (!lenderPincode) {
        throw new Error("Lender pincode could not be loaded. Make sure lender profile endpoint is working.");
      }

      const payload = buildPayload();

      let response;
      if (editingPlanId) {
        response = await api.put(`/loan-plans/lender/${userId}/plan/${editingPlanId}`, payload);
      } else {
        response = await api.post(`/loan-plans/lender/${userId}`, payload);
      }

      setMessage(response.data?.message || (editingPlanId ? "Loan plan updated." : "Loan plan created."));
      resetForm();
      await loadPlans(false);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to save loan plan.");
    } finally {
      setSaving(false);
    }
  }

  useEffect(() => {
    loadPlans(false);
  }, [userId]);

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading lender loan plans...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Lender Loan Plans</h2>
        <p className="mt-2 text-sm text-slate-500">
          Create simple loan plans using only plan name, principal amount, upfront deduction %, and tenure.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard
          title="Total Plans"
          value={plans.length}
          subtitle="All plans created by this lender"
        />
        <SummaryCard
          title="Active Plans"
          value={activePlansCount}
          subtitle="Backend currently marks created/updated plans as ACTIVE"
        />
        <SummaryCard
          title="Average Plan Amount"
          value={averagePlanAmount}
          subtitle="Average principal across all lender plans"
        />
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
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">
            {editingPlanId ? "Edit Loan Plan" : "Create Loan Plan"}
          </h3>

          {editingPlanId ? (
            <button
              type="button"
              onClick={resetForm}
              className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Cancel Edit
            </button>
          ) : null}
        </div>

        <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Plan Name *</label>
            <input
              name="planName"
              value={form.planName}
              onChange={handleChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Starter Plan"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Principal Amount *</label>
            <input
              type="number"
              name="amount"
              value={form.amount}
              onChange={handleChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
              placeholder="100000"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">
              Upfront Deduction % *
            </label>
            <input
              type="number"
              step="0.01"
              name="interestPerDay"
              value={form.interestPerDay}
              onChange={handleChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
              placeholder="7"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Tenure (days) *</label>
            <input
              type="number"
              name="planDuration"
              value={form.planDuration}
              onChange={handleChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
              placeholder="100"
            />
          </div>

          <div className="md:col-span-2 rounded-2xl bg-slate-50 p-4">
            <h4 className="mb-3 text-base font-semibold text-slate-900">Plan Preview</h4>

            {!preview ? (
              <p className="text-sm text-slate-500">
                Enter principal amount, upfront deduction %, and tenure to see borrower and lender projections.
              </p>
            ) : (
              <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
                <div>
                  <p className="text-sm text-slate-500">Borrower Receives</p>
                  <p className="font-semibold text-slate-900">{preview.disbursedAmount}</p>
                </div>

                <div>
                  <p className="text-sm text-slate-500">Upfront Deduction Amount</p>
                  <p className="font-semibold text-slate-900">{preview.upfrontInterest}</p>
                </div>

                <div>
                  <p className="text-sm text-slate-500">Daily EMI</p>
                  <p className="font-semibold text-slate-900">{preview.dailyEmi}</p>
                </div>

                <div>
                  <p className="text-sm text-slate-500">Total Repayable</p>
                  <p className="font-semibold text-slate-900">{preview.totalRepayableAmount}</p>
                </div>

                <div>
                  <p className="text-sm text-slate-500">Lender Gross Profit</p>
                  <p className="font-semibold text-slate-900">{preview.lenderGrossProfit}</p>
                </div>

                <div>
                  <p className="text-sm text-slate-500">Estimated Platform Fee (1%)</p>
                  <p className="font-semibold text-slate-900">{preview.estimatedPlatformFee}</p>
                </div>
              </div>
            )}
          </div>

          <div className="md:col-span-2 flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-60"
            >
              {saving
                ? editingPlanId
                  ? "Updating Plan..."
                  : "Creating Plan..."
                : editingPlanId
                ? "Update Loan Plan"
                : "Create Loan Plan"}
            </button>
          </div>
        </form>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Existing Loan Plans</h3>
          <button
            type="button"
            onClick={() => loadPlans(true)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
          >
            Refresh
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Plan</th>
                <th className="px-3 py-3">Principal</th>
                <th className="px-3 py-3">Upfront %</th>
                <th className="px-3 py-3">Tenure</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {plans.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-3 py-6 text-center text-slate-500">
                    No loan plans found yet.
                  </td>
                </tr>
              ) : (
                plans.map((plan) => (
                  <tr key={plan.id} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{plan.planName}</td>
                    <td className="px-3 py-3 text-slate-700">{plan.amount}</td>
                    <td className="px-3 py-3 text-slate-700">{plan.interestPerDay}</td>
                    <td className="px-3 py-3 text-slate-700">{plan.planDuration}</td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill
                        text={plan.status || "UNKNOWN"}
                        tone={plan.status === "ACTIVE" ? "green" : "yellow"}
                      />
                    </td>
                    <td className="px-3 py-3">
                      <button
                        type="button"
                        onClick={() => startEdit(plan)}
                        className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-medium text-slate-700"
                      >
                        Edit
                      </button>
                    </td>
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