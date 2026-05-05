import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import AppShell from "./layout/AppShell";

import LoginPage from "./pages/auth/LoginPage";
import TermsPage from "./pages/auth/TermsPage";
import BorrowerRegisterPage from "./pages/auth/BorrowerRegisterPage";
import LenderRegisterPage from "./pages/auth/LenderRegisterPage";

import BorrowerDashboard from "./pages/borrower/BorrowerDashboard";
import LenderDashboard from "./pages/lender/LenderDashboard";
import AdminDashboard from "./pages/admin/AdminDashboard";

import KycCenterPage from "./pages/common/KycCenterPage";
import WalletPage from "./pages/common/WalletPage";
import LocationPage from "./pages/common/LocationPage";
import ReportsPage from "./pages/common/ReportsPage";
import NotificationsPage from "./pages/common/NotificationsPage";
import LenderLoanPlansPage from "./pages/lender/LenderLoanPlansPage";
import BorrowerLoanApplicationPage from "./pages/borrower/BorrowerLoanApplicationPage";
import LenderLoanApplicationsReviewPage from "./pages/lender/LenderLoanApplicationsReviewPage";
import BorrowerRepaymentPage from "./pages/borrower/BorrowerRepaymentPage";
import LenderRepaymentTrackingPage from "./pages/lender/LenderRepaymentTrackingPage";

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/terms" element={<TermsPage />} />
          <Route path="/borrower/register" element={<BorrowerRegisterPage />} />
          <Route path="/lender/register" element={<LenderRegisterPage />} />

          <Route
            element={
              <ProtectedRoute allowedRoles={["BORROWER", "LENDER", "ADMIN"]}>
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route
              path="/borrower/dashboard"
              element={
                <ProtectedRoute allowedRoles={["BORROWER"]}>
                  <BorrowerDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/lender/dashboard"
              element={
                <ProtectedRoute allowedRoles={["LENDER"]}>
                  <LenderDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/dashboard"
              element={
                <ProtectedRoute allowedRoles={["ADMIN"]}>
                  <AdminDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/kyc"
              element={
                <ProtectedRoute allowedRoles={["BORROWER", "LENDER"]}>
                  <KycCenterPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/wallet"
              element={
                <ProtectedRoute allowedRoles={["BORROWER", "LENDER"]}>
                  <WalletPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/location"
              element={
                <ProtectedRoute allowedRoles={["BORROWER", "LENDER"]}>
                  <LocationPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reports"
              element={
                <ProtectedRoute allowedRoles={["BORROWER", "LENDER"]}>
                  <ReportsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/notifications"
              element={
                <ProtectedRoute allowedRoles={["BORROWER", "LENDER", "ADMIN"]}>
                  <NotificationsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/lender/loan-plans"
              element={
                <ProtectedRoute allowedRoles={["LENDER"]}>
                  <LenderLoanPlansPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/borrower/loan-apply"
              element={
                <ProtectedRoute allowedRoles={["BORROWER"]}>
                  <BorrowerLoanApplicationPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/lender/review-applications"
              element={
                <ProtectedRoute allowedRoles={["LENDER"]}>
                  <LenderLoanApplicationsReviewPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/borrower/repayments"
              element={
                <ProtectedRoute allowedRoles={["BORROWER"]}>
                  <BorrowerRepaymentPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/lender/repayments"
              element={
                <ProtectedRoute allowedRoles={["LENDER"]}>
                  <LenderRepaymentTrackingPage />
                </ProtectedRoute>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}