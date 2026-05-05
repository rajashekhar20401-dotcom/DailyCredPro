import { useMemo, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../../api/client";
import termsContent from "../../content/termsContent";

const employmentOptions = [
  "SELF_EMPLOYED",
  "PRIVATE_EMPLOYEE",
  "GOVERNMENT_EMPLOYEE",
  "BUSINESS_OWNER",
  "DAILY_WAGE",
  "OTHER",
];

const propertyTypeOptions = [
  "HOUSE",
  "LAND",
  "SHOP",
  "VEHICLE",
  "NONE",
  "OTHER",
];

const collateralTypeOptions = [
  "GOLD",
  "LAND_DOCUMENTS",
  "HOUSE_DOCUMENTS",
  "VEHICLE_DOCUMENTS",
  "NONE",
  "OTHER",
];

const initialForm = {
  borrowerName: "",
  dateOfBirth: "",
  password: "",
  confirmPassword: "",
  isActive: true,
  phoneNumber: "",
  pincode: "",
  address: "",
  aadharCardNumber: "",
  panCardNumber: "",
  email: "",
  notificationEnabled: true,
  emailNotificationsEnabled: true,
  termsAccepted: false,
  termsVersion: "v1",
  monthlyIncome: "",
  incomeType: "MONTHLY",
  employmentType: "SELF_EMPLOYED",
  yearsInCurrentWork: "",
  dependentsCount: "",
  houseOwned: false,
  shopOwned: false,
  incomeProofUploaded: false,
  propertyOwned: false,
  propertyType: "NONE",
  collateralProvided: false,
  collateralType: "NONE",
  kycCompletionPercent: 0,
  kycVerified: false,
};

export default function BorrowerRegisterPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [showTermsModal, setShowTermsModal] = useState(false);

  function handleChange(event) {
    const { name, value, type, checked } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  const passwordMismatch = useMemo(() => {
    return form.confirmPassword && form.password !== form.confirmPassword;
  }, [form.password, form.confirmPassword]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!form.termsAccepted) {
      setError("You must accept the terms and conditions.");
      return;
    }

    if (form.password !== form.confirmPassword) {
      setError("Password and Confirm Password must match.");
      return;
    }

    setLoading(true);

    try {
      const payload = {
        borrowerName: form.borrowerName,
        dateOfBirth: form.dateOfBirth,
        password: form.password,
        isActive: form.isActive,
        phoneNumber: form.phoneNumber,
        pincode: form.pincode,
        address: form.address,
        aadharCardNumber: form.aadharCardNumber,
        panCardNumber: form.panCardNumber,
        email: form.email,
        notificationEnabled: form.notificationEnabled,
        emailNotificationsEnabled: form.emailNotificationsEnabled,
        termsAccepted: form.termsAccepted,
        termsVersion: form.termsVersion,
        monthlyIncome: form.monthlyIncome ? Number(form.monthlyIncome) : null,
        incomeType: form.incomeType,
        employmentType: form.employmentType,
        yearsInCurrentWork: form.yearsInCurrentWork ? Number(form.yearsInCurrentWork) : null,
        dependentsCount: form.dependentsCount ? Number(form.dependentsCount) : null,
        houseOwned: form.houseOwned,
        shopOwned: form.shopOwned,
        incomeProofUploaded: form.incomeProofUploaded,
        propertyOwned: form.propertyOwned,
        propertyType: form.propertyType,
        collateralProvided: form.collateralProvided,
        collateralType: form.collateralType,
        kycCompletionPercent: 0,
        kycVerified: false,
      };

      const response = await api.post("/api/borrowers", payload);

      setSuccess(response.data?.message || "Borrower registered successfully.");

      // NOTE:
      // As soon as borrower is created, backend should create an in-app notification
      // for that borrower (and optionally an email later).
      // We should add that in BorrowerServiceImpl / NotificationService integration.

      setTimeout(() => {
        navigate("/login");
      }, 1200);
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Borrower registration failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <div className="min-h-screen bg-slate-100 px-4 py-10">
        <div className="mx-auto max-w-5xl rounded-3xl bg-white p-8 shadow-lg">
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-slate-900">Borrower Registration</h1>
            <p className="mt-2 text-sm text-slate-500">
              Create your borrower account to apply for loans, upload KYC, and track repayments.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-5 md:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Full Name</label>
              <input
                name="borrowerName"
                value={form.borrowerName}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Ravi Kumar"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Date of Birth</label>
              <input
                type="date"
                name="dateOfBirth"
                value={form.dateOfBirth}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Phone Number</label>
              <input
                name="phoneNumber"
                value={form.phoneNumber}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="9000000001"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Email</label>
              <input
                name="email"
                value={form.email}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="ravi@example.com"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Password</label>
              <div className="flex gap-2">
                <input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                  placeholder="Enter password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="rounded-xl border border-slate-300 px-4 py-2 text-sm"
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Confirm Password</label>
              <div className="flex gap-2">
                <input
                  type={showConfirmPassword ? "text" : "password"}
                  name="confirmPassword"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2"
                  placeholder="Retype password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword((prev) => !prev)}
                  className="rounded-xl border border-slate-300 px-4 py-2 text-sm"
                >
                  {showConfirmPassword ? "Hide" : "Show"}
                </button>
              </div>
              {passwordMismatch ? (
                <p className="mt-1 text-sm text-red-600">Passwords do not match.</p>
              ) : null}
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Pincode</label>
              <input
                name="pincode"
                value={form.pincode}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="500001"
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
                placeholder="25000"
              />
            </div>

            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium text-slate-700">Address</label>
              <textarea
                name="address"
                value={form.address}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                rows="3"
                placeholder="Enter full address"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Aadhaar Number</label>
              <input
                name="aadharCardNumber"
                value={form.aadharCardNumber}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="123456789012"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">PAN Number</label>
              <input
                name="panCardNumber"
                value={form.panCardNumber}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="ABCDE1234F"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Income Type</label>
              <select
                name="incomeType"
                value={form.incomeType}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
              >
                <option value="MONTHLY">MONTHLY</option>
                <option value="DAILY">DAILY</option>
                <option value="WEEKLY">WEEKLY</option>
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Employment Type</label>
              <select
                name="employmentType"
                value={form.employmentType}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
              >
                {employmentOptions.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Years in Current Work</label>
              <input
                type="number"
                name="yearsInCurrentWork"
                value={form.yearsInCurrentWork}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="3"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Dependents Count</label>
              <input
                type="number"
                name="dependentsCount"
                value={form.dependentsCount}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Property Type</label>
              <select
                name="propertyType"
                value={form.propertyType}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
              >
                {propertyTypeOptions.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Collateral Type</label>
              <select
                name="collateralType"
                value={form.collateralType}
                onChange={handleChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
              >
                {collateralTypeOptions.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </div>

            <div className="rounded-2xl border border-slate-200 p-4 md:col-span-2">
              <h2 className="mb-3 text-lg font-semibold text-slate-800">Preferences & Declarations</h2>

              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input type="checkbox" name="houseOwned" checked={form.houseOwned} onChange={handleChange} />
                  House Owned
                </label>

                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input type="checkbox" name="shopOwned" checked={form.shopOwned} onChange={handleChange} />
                  Shop Owned
                </label>

                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    name="incomeProofUploaded"
                    checked={form.incomeProofUploaded}
                    onChange={handleChange}
                  />
                  Income Proof Uploaded
                </label>

                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input type="checkbox" name="propertyOwned" checked={form.propertyOwned} onChange={handleChange} />
                  Property Owned
                </label>

                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    name="collateralProvided"
                    checked={form.collateralProvided}
                    onChange={handleChange}
                  />
                  Collateral Provided
                </label>

                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    name="notificationEnabled"
                    checked={form.notificationEnabled}
                    onChange={handleChange}
                  />
                  Enable In-App Notifications
                </label>

                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    name="emailNotificationsEnabled"
                    checked={form.emailNotificationsEnabled}
                    onChange={handleChange}
                  />
                  Enable Email Notifications
                </label>

                <label className="flex items-center gap-2 text-sm text-slate-700">
                  <input type="checkbox" name="isActive" checked={form.isActive} onChange={handleChange} />
                  Account Active
                </label>
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 p-4 md:col-span-2">
              <label className="flex items-start gap-3 text-sm text-slate-700">
                <input
                  type="checkbox"
                  name="termsAccepted"
                  checked={form.termsAccepted}
                  onChange={handleChange}
                  className="mt-1"
                />
                <span>
                  I have read and accepted the{" "}
                  <button
                    type="button"
                    onClick={() => setShowTermsModal(true)}
                    className="font-medium text-blue-600 underline"
                  >
                    Terms & Conditions
                  </button>{" "}
                  (version {form.termsVersion}).
                </span>
              </label>
            </div>

            {error ? (
              <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600 md:col-span-2">{error}</div>
            ) : null}

            {success ? (
              <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700 md:col-span-2">{success}</div>
            ) : null}

            <div className="flex items-center justify-between md:col-span-2">
              <Link to="/login" className="text-sm font-medium text-slate-600 hover:underline">
                Back to Login
              </Link>

              <button
                type="submit"
                disabled={loading}
                className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-60"
              >
                {loading ? "Creating Account..." : "Create Borrower Account"}
              </button>
            </div>
          </form>
        </div>
      </div>

      {showTermsModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="max-h-[85vh] w-full max-w-3xl overflow-hidden rounded-3xl bg-white shadow-2xl">
            <div className="flex items-center justify-between border-b px-6 py-4">
              <h2 className="text-xl font-semibold text-slate-900">Terms & Conditions</h2>
              <button
                type="button"
                onClick={() => setShowTermsModal(false)}
                className="rounded-xl border border-slate-300 px-4 py-2 text-sm"
              >
                Close
              </button>
            </div>

            <div className="max-h-[70vh] overflow-y-auto px-6 py-5">
              <pre className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
                {termsContent}
              </pre>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}