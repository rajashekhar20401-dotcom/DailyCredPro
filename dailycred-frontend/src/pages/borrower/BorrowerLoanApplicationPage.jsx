import { useEffect, useMemo, useState } from "react";
import api from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

const employeeTypeOptions = [
  "SALARIED",
  "SELF_EMPLOYED",
  "BUSINESS",
  "DAILY_WAGE",
];

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

function calculateAge(dateOfBirth) {
  if (!dateOfBirth) return "";
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return "";

  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
    age--;
  }

  return age;
}

function getPlanPreview(plan) {
  const principal = Number(plan.amount || 0);
  const upfrontPercent = Number(plan.interestPerDay || 0);
  const tenure = Number(plan.planDuration || 0);

  if (!principal || !upfrontPercent || !tenure) return null;

  const upfrontInterest = Number(((principal * upfrontPercent) / 100).toFixed(2));
  const disbursedAmount = Number((principal - upfrontInterest).toFixed(2));
  const dailyEmi = Number((principal / tenure).toFixed(2));

  return {
    upfrontInterest,
    disbursedAmount,
    totalRepayable: principal,
    dailyEmi,
  };
}

function normalizeText(value) {
  return value == null ? "" : String(value).trim().toUpperCase();
}

function getEligibility(plan, borrowerProfile, borrowerAnalytics) {
  const reasons = [];

  const eligibilityStatus = normalizeText(borrowerAnalytics?.eligibilityStatus);
  const riskCategory = normalizeText(borrowerAnalytics?.riskCategory);
  const allowedActiveLoanLimit = Number(borrowerAnalytics?.allowedActiveLoanLimit ?? 0);
  const remainingActiveLoanSlots = Number(borrowerAnalytics?.remainingActiveLoanSlots ?? 0);
  const activeLoans = Number(borrowerAnalytics?.activeLoans ?? 0);
  const maxEligibleLoanAmount = Number(borrowerAnalytics?.maxEligibleLoanAmount ?? 0);

  if (!plan || normalizeText(plan.status) !== "ACTIVE") {
    reasons.push("Plan is not active.");
  }

  if (allowedActiveLoanLimit > 0 && activeLoans >= allowedActiveLoanLimit) {
    reasons.push(`You already reached your active loan limit of ${allowedActiveLoanLimit}.`);
  }

  if (remainingActiveLoanSlots <= 0) {
    reasons.push("No active loan slots remaining.");
  }

  if (maxEligibleLoanAmount > 0 && Number(plan.amount) > maxEligibleLoanAmount) {
    reasons.push(`This plan exceeds your current eligible cap of ${maxEligibleLoanAmount}.`);
  }

  if (
    eligibilityStatus === "NOT_ELIGIBLE" ||
    eligibilityStatus === "PENDING_REVIEW"
  ) {
    reasons.push(`Borrower status is currently ${eligibilityStatus}.`);
  }

  const borrowerIncome = Number(borrowerProfile?.monthlyIncome ?? 0);
  if (plan.minMonthlyIncome != null && borrowerIncome < Number(plan.minMonthlyIncome)) {
    reasons.push("Monthly income is below the required minimum.");
  }

  const borrowerAge = calculateAge(borrowerProfile?.dateOfBirth);
  if (plan.minAge != null && borrowerAge < Number(plan.minAge)) {
    reasons.push("Borrower age is below the allowed minimum.");
  }

  if (plan.maxAge != null && borrowerAge > Number(plan.maxAge)) {
    reasons.push("Borrower age is above the allowed maximum.");
  }

  if (riskCategory === "HIGH" && Number(plan.amount) > maxEligibleLoanAmount && maxEligibleLoanAmount > 0) {
    reasons.push("High-risk profile cannot access plans above the current cap.");
  }

  return {
    eligible: reasons.length === 0,
    reasons,
  };
}

export default function BorrowerLoanApplicationPage() {
  const { userId } = useAuth();

  const [borrowerProfile, setBorrowerProfile] = useState(null);
  const [borrowerAnalytics, setBorrowerAnalytics] = useState(null);

  const radiusKm = 30;
  const [nearbyLenders, setNearbyLenders] = useState([]);
  const [selectedLender, setSelectedLender] = useState(null);
  const [selectedLenderProfile, setSelectedLenderProfile] = useState(null);

  const [lenderPlans, setLenderPlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);

  const [applications, setApplications] = useState([]);

  const [form, setForm] = useState({
    age: "",
    monthlyIncome: "",
    employeeType: "SELF_EMPLOYED",
    pinCode: "",
    collateral: "",
  });

  const [locationStatus, setLocationStatus] = useState("Location not fetched yet.");
  const [loadingContext, setLoadingContext] = useState(true);
  const [loadingNearby, setLoadingNearby] = useState(false);
  const [loadingPlans, setLoadingPlans] = useState(false);
  const [loadingApplications, setLoadingApplications] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [message, setMessage] = useState("");
  const [pageError, setPageError] = useState("");

  const eligiblePlans = useMemo(() => {
    return lenderPlans.filter((plan) => getEligibility(plan, borrowerProfile, borrowerAnalytics).eligible);
  }, [lenderPlans, borrowerProfile, borrowerAnalytics]);

  const collateralRequiredForCurrentBorrower = useMemo(() => {
    return (
      borrowerAnalytics?.collateralRequired === true ||
      normalizeText(borrowerAnalytics?.eligibilityStatus) === "COLLATERAL_REQUIRED" ||
      normalizeText(borrowerAnalytics?.riskCategory) === "HIGH"
    );
  }, [borrowerAnalytics]);

  async function loadBorrowerContext() {
    if (!userId) return;

    setLoadingContext(true);
    setPageError("");

    try {
      const [borrowerResponse, analyticsResponse] = await Promise.all([
        api.get(`/api/borrowers/${userId}`),
        api.get(`/api/borrower-analytics/${userId}/summary`),
      ]);

      const borrower = borrowerResponse.data?.data || null;
      const analytics = analyticsResponse.data?.data || null;

      setBorrowerProfile(borrower);
      setBorrowerAnalytics(analytics);

      setForm((prev) => ({
        ...prev,
        age: calculateAge(borrower?.dateOfBirth),
        monthlyIncome: borrower?.monthlyIncome ? Number(borrower.monthlyIncome) : "",
        employeeType: borrower?.employmentType || "SELF_EMPLOYED",
        pinCode: borrower?.pincode || "",
      }));
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load borrower profile.");
    } finally {
      setLoadingContext(false);
    }
  }

  async function loadBorrowerApplications(showMessage = false) {
    if (!userId) return;

    setLoadingApplications(true);

    try {
      const response = await api.get(`/api/loan-application/lenderApplications/borrowerId/${userId}`);
      setApplications(response.data?.data || []);

      if (showMessage) {
        setMessage("Borrower applications refreshed successfully.");
      }
    } catch {
      setApplications([]);
    } finally {
      setLoadingApplications(false);
    }
  }

  function getCurrentBrowserLocation() {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("Geolocation is not supported in this browser."));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          if (error.code === 1) {
            reject(new Error("Location permission was denied."));
          } else if (error.code === 2) {
            reject(new Error("Unable to detect current location."));
          } else if (error.code === 3) {
            reject(new Error("Location request timed out."));
          } else {
            reject(new Error("Failed to fetch current location."));
          }
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0,
        }
      );
    });
  }

  async function saveBorrowerLocation(latitude, longitude) {
    await api.put(`/api/location/borrowers/${userId}`, {
      latitude,
      longitude,
      consentGiven: true,
    });
  }

  async function findNearbyLenders() {
    setLoadingNearby(true);
    setPageError("");
    setMessage("");
    setNearbyLenders([]);
    setSelectedLender(null);
    setSelectedLenderProfile(null);
    setLenderPlans([]);
    setSelectedPlan(null);

    try {
      setLocationStatus("Requesting browser location permission...");

      const coords = await getCurrentBrowserLocation();

      setLocationStatus(
        `Location captured successfully. Latitude: ${coords.latitude.toFixed(5)}, Longitude: ${coords.longitude.toFixed(5)}`
      );

      await saveBorrowerLocation(coords.latitude, coords.longitude);

      setLocationStatus("Location saved successfully. Finding nearby lenders within 30 km...");

      const response = await api.get(`/api/location/borrowers/${userId}/nearby-lenders`, {
        params: { radiusKm: Number(radiusKm) },
      });

      setNearbyLenders(response.data?.data || []);
      setMessage("Nearby lenders loaded successfully.");
      setLocationStatus("Nearby lender search completed.");
    } catch (err) {
      setLocationStatus("Location fetch failed.");
      setPageError(err.response?.data?.message || err.message || "Failed to load nearby lenders.");
    } finally {
      setLoadingNearby(false);
    }
  }

  async function selectLenderAndLoadPlans(lender) {
    setSelectedLender(lender);
    setSelectedPlan(null);
    setLoadingPlans(true);
    setPageError("");
    setMessage("");

    try {
      const [plansResponse, lenderProfileResponse] = await Promise.all([
        api.get(`/loan-plans/lender/${lender.lenderId}`),
        api.get(`/api/lenders/${lender.lenderId}`),
      ]);

      setLenderPlans(plansResponse.data?.data || []);
      setSelectedLenderProfile(lenderProfileResponse.data?.data || null);
      setMessage("Lender plans loaded successfully.");
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load selected lender plans.");
      setLenderPlans([]);
      setSelectedLenderProfile(null);
    } finally {
      setLoadingPlans(false);
    }
  }

  function handleChange(event) {
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  async function handleApply(event) {
    event.preventDefault();
    setPageError("");
    setMessage("");

    if (!selectedLender || !selectedPlan) {
      setPageError("Please select an eligible lender plan first.");
      return;
    }

    const eligibilityCheck = getEligibility(selectedPlan, borrowerProfile, borrowerAnalytics);
    if (!eligibilityCheck.eligible) {
      setPageError(eligibilityCheck.reasons[0] || "You are not eligible for this plan.");
      return;
    }

    setSubmitting(true);

    try {
      const payload = {
        loanAmount: selectedPlan.amount,
        age: form.age === "" ? null : Number(form.age),
        monthlyIncome: form.monthlyIncome === "" ? null : Number(form.monthlyIncome),
        employeeType: form.employeeType,
        pinCode: form.pinCode,
        isEducated: false,
        certificates: null,
        collateral: collateralRequiredForCurrentBorrower ? form.collateral : null,
      };

      const response = await api.post(
        `/api/loan-application/${userId}/${selectedLender.lenderId}/${selectedPlan.id}`,
        payload
      );

      setMessage(response.data?.message || "Loan application submitted successfully.");
      await loadBorrowerApplications(false);
      await loadBorrowerContext();
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to submit loan application.");
    } finally {
      setSubmitting(false);
    }
  }

  useEffect(() => {
    loadBorrowerContext();
    loadBorrowerApplications(false);
  }, [userId]);

  if (loadingContext) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading borrower discover & apply center...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Discover Lenders & Apply</h2>
        <p className="mt-2 text-sm text-slate-500">
          Click the button below to allow location access, save your current location, and find lenders within a fixed 30 km radius.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3 xl:grid-cols-6">
        <SummaryCard
          title="Internal Score"
          value={borrowerAnalytics?.internalCreditScore ?? 0}
          subtitle="Used in platform eligibility logic"
        />
        <SummaryCard
          title="Risk Category"
          value={borrowerAnalytics?.riskCategory || "UNKNOWN"}
          subtitle={`Risk Score: ${borrowerAnalytics?.riskScore ?? 0}`}
        />
        <SummaryCard
          title="Eligibility"
          value={borrowerAnalytics?.eligibilityTier || "UNASSIGNED"}
          subtitle={borrowerAnalytics?.eligibilityStatus || "PENDING_REVIEW"}
        />
        <SummaryCard
          title="Max Eligible Amount"
          value={borrowerAnalytics?.maxEligibleLoanAmount ?? 0}
          subtitle="Current borrower limit"
        />
        <SummaryCard
          title="Loan Limit"
          value={borrowerAnalytics?.allowedActiveLoanLimit ?? 0}
          subtitle={`Active loans: ${borrowerAnalytics?.activeLoans ?? 0}`}
        />
        <SummaryCard
          title="Remaining Slots"
          value={borrowerAnalytics?.remainingActiveLoanSlots ?? 0}
          subtitle={borrowerAnalytics?.collateralRequired ? "Collateral likely required" : "Standard flow available"}
        />
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-3 text-lg font-semibold text-slate-900">Current Restrictions</h3>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700">
            <p className="font-medium text-slate-900">Allowed Active Loans</p>
            <p className="mt-1">{borrowerAnalytics?.allowedActiveLoanLimit ?? 0}</p>
          </div>
          <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700">
            <p className="font-medium text-slate-900">Remaining Loan Slots</p>
            <p className="mt-1">{borrowerAnalytics?.remainingActiveLoanSlots ?? 0}</p>
          </div>
          <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700">
            <p className="font-medium text-slate-900">Collateral Requirement</p>
            <p className="mt-1">
              {borrowerAnalytics?.collateralRequired ? "Required / likely required" : "Not required right now"}
            </p>
          </div>
          <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-700">
            <p className="font-medium text-slate-900">Recommendation</p>
            <p className="mt-1">{borrowerAnalytics?.recommendation || "No recommendation available."}</p>
          </div>
        </div>
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
        <div className="mb-4 flex items-center gap-3">
          <button
            type="button"
            onClick={findNearbyLenders}
            disabled={loadingNearby}
            className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-medium text-white disabled:opacity-60"
          >
            {loadingNearby ? "Fetching Location & Searching..." : "Find Nearby Lenders (30 km)"}
          </button>
        </div>

        <div className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-700">
          {locationStatus}
        </div>

        <div className="mt-5 grid grid-cols-1 gap-4 lg:grid-cols-2">
          {nearbyLenders.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500 lg:col-span-2">
              No nearby lenders loaded yet. Click the button above and allow browser location access.
            </div>
          ) : (
            nearbyLenders.map((lender) => (
              <button
                type="button"
                key={lender.lenderId}
                onClick={() => selectLenderAndLoadPlans(lender)}
                className={`rounded-2xl border p-5 text-left transition ${
                  selectedLender?.lenderId === lender.lenderId
                    ? "border-slate-900 bg-slate-50"
                    : "border-slate-200 bg-white hover:bg-slate-50"
                }`}
              >
                <h3 className="text-lg font-semibold text-slate-900">{lender.lenderName}</h3>
                <p className="mt-2 text-sm text-slate-600">Lender ID: {lender.lenderId}</p>
                <p className="text-sm text-slate-600">Pincode: {lender.pincode}</p>
                <p className="text-sm text-slate-600">Distance: {lender.distanceKm} km</p>
              </button>
            ))
          )}
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">
          {selectedLender ? `Eligible Plans from ${selectedLender.lenderName}` : "Eligible Loan Plans"}
        </h3>

        {loadingPlans ? (
          <p className="text-sm text-slate-500">Loading lender plans...</p>
        ) : !selectedLender ? (
          <p className="text-sm text-slate-500">Select a nearby lender first.</p>
        ) : eligiblePlans.length === 0 ? (
          <div className="space-y-3">
            <p className="text-sm text-red-600">
              No eligible plans are visible for this borrower under the current platform eligibility rules.
            </p>

            {lenderPlans.length > 0 ? (
              <div className="grid grid-cols-1 gap-3">
                {lenderPlans.map((plan) => {
                  const check = getEligibility(plan, borrowerProfile, borrowerAnalytics);
                  return (
                    <div key={plan.id} className="rounded-xl border border-slate-200 p-4 text-sm">
                      <p className="font-medium text-slate-900">
                        {plan.planName} — {plan.amount}
                      </p>
                      <ul className="mt-2 list-disc space-y-1 pl-5 text-red-600">
                        {check.reasons.map((reason, index) => (
                          <li key={`${plan.id}-${index}`}>{reason}</li>
                        ))}
                      </ul>
                    </div>
                  );
                })}
              </div>
            ) : null}
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {eligiblePlans.map((plan) => {
              const preview = getPlanPreview(plan);

              return (
                <button
                  type="button"
                  key={plan.id}
                  onClick={() => setSelectedPlan(plan)}
                  className={`rounded-2xl border p-5 text-left transition ${
                    selectedPlan?.id === plan.id
                      ? "border-slate-900 bg-slate-50"
                      : "border-slate-200 bg-white hover:bg-slate-50"
                  }`}
                >
                  <div className="mb-3 flex items-center justify-between">
                    <h4 className="text-lg font-semibold text-slate-900">{plan.planName}</h4>
                    <StatusPill text={plan.status} tone="green" />
                  </div>

                  <div className="grid grid-cols-2 gap-3 text-sm text-slate-700">
                    <div>
                      <p className="text-slate-500">Principal</p>
                      <p className="font-medium">{plan.amount}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Upfront %</p>
                      <p className="font-medium">{plan.interestPerDay}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Tenure</p>
                      <p className="font-medium">{plan.planDuration} days</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Age Rule</p>
                      <p className="font-medium">
                        {plan.minAge} - {plan.maxAge}
                      </p>
                    </div>

                    <div>
                      <p className="text-slate-500">Borrower Receives</p>
                      <p className="font-medium">{preview?.disbursedAmount ?? "-"}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Daily EMI</p>
                      <p className="font-medium">{preview?.dailyEmi ?? "-"}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Total Repayable</p>
                      <p className="font-medium">{preview?.totalRepayable ?? "-"}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Income Required</p>
                      <p className="font-medium">{plan.minMonthlyIncome ?? 0}</p>
                    </div>
                  </div>

                  <div className="mt-4 rounded-xl bg-white p-3">
                    <p className="text-xs font-medium text-slate-500">Lender Contact</p>
                    <p className="mt-1 text-sm text-slate-700">
                      {selectedLenderProfile?.lenderName || selectedLender.lenderName}
                    </p>
                    <p className="text-sm text-slate-700">
                      {selectedLenderProfile?.phoneNumber || "-"} | {selectedLenderProfile?.email || "-"}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">Apply for Selected Plan</h3>

        {!selectedPlan ? (
          <p className="text-sm text-slate-500">Select one eligible plan first.</p>
        ) : (
          <>
            <div className="mb-5 rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Selected Plan</p>
              <p className="mt-1 font-semibold text-slate-900">
                {selectedPlan.planName} | Principal: {selectedPlan.amount} | Upfront %: {selectedPlan.interestPerDay} |
                Tenure: {selectedPlan.planDuration} days
              </p>
            </div>

            <form onSubmit={handleApply} className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Loan Amount</label>
                <input
                  type="text"
                  value={selectedPlan?.amount ?? ""}
                  readOnly
                  className="w-full rounded-xl border border-slate-300 bg-slate-100 px-3 py-2 text-slate-600"
                />
                <p className="mt-1 text-xs text-slate-500">
                  This amount is fixed by the selected loan plan.
                </p>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Age</label>
                <input
                  type="number"
                  name="age"
                  value={form.age}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Monthly Income</label>
                <input
                  type="number"
                  name="monthlyIncome"
                  value={form.monthlyIncome}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Employee Type</label>
                <select
                  name="employeeType"
                  value={form.employeeType}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                >
                  {employeeTypeOptions.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">Pin Code</label>
                <input
                  name="pinCode"
                  value={form.pinCode}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                />
              </div>

              {collateralRequiredForCurrentBorrower ? (
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm font-medium text-slate-700">
                    Collateral <span className="text-red-600">*</span>
                  </label>
                  <input
                    name="collateral"
                    value={form.collateral}
                    onChange={handleChange}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2"
                    placeholder="Gold / bond / land proof / vehicle documents"
                  />
                  <p className="mt-1 text-xs text-amber-700">
                    Collateral is required because your current eligibility status requires collateral-backed lending.
                  </p>
                </div>
              ) : null}

              <div className="md:col-span-2 flex justify-end">
                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-60"
                >
                  {submitting ? "Submitting Application..." : "Submit Loan Application"}
                </button>
              </div>
            </form>
          </>
        )}
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">My Loan Applications</h3>
          <button
            type="button"
            onClick={() => loadBorrowerApplications(true)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
          >
            Refresh
          </button>
        </div>

        {loadingApplications ? (
          <p className="text-sm text-slate-500">Loading applications...</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-3 py-3">Application ID</th>
                  <th className="px-3 py-3">Plan ID</th>
                  <th className="px-3 py-3">Lender ID</th>
                  <th className="px-3 py-3">Loan Amount</th>
                  <th className="px-3 py-3">Employee Type</th>
                  <th className="px-3 py-3">Pin Code</th>
                  <th className="px-3 py-3">Status</th>
                  <th className="px-3 py-3">Applied At</th>
                </tr>
              </thead>
              <tbody>
                {applications.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="px-3 py-6 text-center text-slate-500">
                      No applications found yet.
                    </td>
                  </tr>
                ) : (
                  applications.map((item) => (
                    <tr key={item.applicationId} className="border-b border-slate-100">
                      <td className="px-3 py-3 text-slate-700">{item.applicationId}</td>
                      <td className="px-3 py-3 text-slate-700">{item.planId}</td>
                      <td className="px-3 py-3 text-slate-700">{item.lenderId}</td>
                      <td className="px-3 py-3 text-slate-700">{item.loanAmount}</td>
                      <td className="px-3 py-3 text-slate-700">{item.employeeType}</td>
                      <td className="px-3 py-3 text-slate-700">{item.pinCode}</td>
                      <td className="px-3 py-3 text-slate-700">
                        <StatusPill
                          text={item.applicationStatus || "UNKNOWN"}
                          tone={
                            item.applicationStatus === "APPROVED"
                              ? "green"
                              : item.applicationStatus === "REJECTED"
                              ? "red"
                              : item.applicationStatus === "PENDING"
                              ? "yellow"
                              : "slate"
                          }
                        />
                      </td>
                      <td className="px-3 py-3 text-slate-700">
                        {item.appliedAt ? new Date(item.appliedAt).toLocaleString() : "-"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}