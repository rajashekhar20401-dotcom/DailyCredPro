package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.dto.responsedto.EligibilityResultDto;
import com.unqiuehire.kashflow.dto.responsedto.RiskAnalysisResultDto;
import com.unqiuehire.kashflow.entity.AdminAccount;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.repository.AdminAccountRepository;
import com.unqiuehire.kashflow.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EligibilityServiceImpl implements EligibilityService {

    /**
     * Final fallback defaults only.
     * Real values should now come from:
     * 1) borrower override
     * 2) admin global policy
     * 3) these defaults
     */
    private static final int DEFAULT_MAX_ACTIVE_LOANS = 3;

    private static final double DEFAULT_PREMIUM_LIMIT = 500000.0;
    private static final double DEFAULT_STANDARD_LIMIT = 300000.0;
    private static final double DEFAULT_BASIC_LIMIT = 100000.0;
    private static final double DEFAULT_LOW_LIMIT = 50000.0;

    private static final int DEFAULT_MANUAL_REVIEW_RISK_THRESHOLD = 60;
    private static final int MIN_BASIC_KYC_PERCENT = 30;

    private final AdminAccountRepository adminAccountRepository;

    @Override
    public EligibilityResultDto evaluate(
            Borrower borrower,
            Integer internalCreditScore,
            RiskAnalysisResultDto riskResult,
            List<Loan> loans
    ) {
        EligibilityResultDto dto = new EligibilityResultDto();

        int score = internalCreditScore == null ? 0 : internalCreditScore;
        int riskScore = riskResult == null || riskResult.getRiskScore() == null ? 100 : riskResult.getRiskScore();
        String riskCategory = riskResult == null || isBlank(riskResult.getRiskCategory())
                ? "HIGH"
                : riskResult.getRiskCategory().trim().toUpperCase();

        AdminAccount policyAdmin = resolvePolicyAdmin();

        int allowedActiveLoans = resolveAllowedActiveLoans(borrower, policyAdmin);

        double premiumLimit = resolvePremiumLimit(borrower, policyAdmin);
        double standardLimit = resolveStandardLimit(policyAdmin);
        double basicLimit = resolveBasicLimit(policyAdmin);
        double lowLimit = resolveLowLimit(policyAdmin);

        int manualReviewRiskThreshold = resolveManualReviewRiskThreshold(policyAdmin);

        int activeLoans = (int) loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .count();

        int totalLoansTaken = borrower.getTotalLoansTaken() == null ? loans.size() : borrower.getTotalLoansTaken();
        int totalMissedDays = borrower.getTotalMissedDays() == null ? 0 : borrower.getTotalMissedDays();
        int maxConsecutiveMissedDays = borrower.getMaxConsecutiveMissedDays() == null ? 0 : borrower.getMaxConsecutiveMissedDays();
        int defaultedLoanCount = borrower.getDefaultedLoanCount() == null ? 0 : borrower.getDefaultedLoanCount();
        int kycCompletionPercent = borrower.getKycCompletionPercent() == null ? 0 : borrower.getKycCompletionPercent();

        BigDecimal monthlyIncome = borrower.getMonthlyIncome() == null ? BigDecimal.ZERO : borrower.getMonthlyIncome();

        boolean fraudFlag = Boolean.TRUE.equals(borrower.getFraudFlag());
        boolean blacklisted = Boolean.TRUE.equals(borrower.getBlacklisted());
        boolean kycVerified = Boolean.TRUE.equals(borrower.getKycVerified());
        boolean incomeProofUploaded = Boolean.TRUE.equals(borrower.getIncomeProofUploaded());
        boolean collateralProvided = Boolean.TRUE.equals(borrower.getCollateralProvided());

        boolean hasIdentityDocument =
                !isBlank(borrower.getAadharCardNumber()) || !isBlank(borrower.getPanCardNumber());

        // 1) Hard stop rules
        if (blacklisted) {
            dto.setEligibilityTier("BLOCKED");
            dto.setEligibilityStatus("NOT_ELIGIBLE");
            dto.setMaxEligibleLoanAmount(0.0);
            dto.setCollateralRequired(false);
            dto.setReason("Borrower is blacklisted and cannot apply for loans.");
            return dto;
        }

        if (fraudFlag) {
            dto.setEligibilityTier("MANUAL_REVIEW");
            dto.setEligibilityStatus("NOT_ELIGIBLE");
            dto.setMaxEligibleLoanAmount(0.0);
            dto.setCollateralRequired(false);
            dto.setReason("Borrower is fraud-flagged and needs admin review.");
            return dto;
        }

        if (!hasIdentityDocument) {
            dto.setEligibilityTier("KYC_MISSING");
            dto.setEligibilityStatus("PENDING_REVIEW");
            dto.setMaxEligibleLoanAmount(0.0);
            dto.setCollateralRequired(false);
            dto.setReason("Add at least Aadhaar or PAN to unlock basic loan eligibility.");
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        if (kycCompletionPercent < MIN_BASIC_KYC_PERCENT) {
            dto.setEligibilityTier("KYC_INCOMPLETE");
            dto.setEligibilityStatus("PENDING_REVIEW");
            dto.setMaxEligibleLoanAmount(0.0);
            dto.setCollateralRequired(false);
            dto.setReason("Complete at least 30% KYC to unlock basic loan eligibility.");
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        if (activeLoans >= allowedActiveLoans) {
            dto.setEligibilityTier("ACTIVE_LOAN_LIMIT");
            dto.setEligibilityStatus("NOT_ELIGIBLE");
            dto.setMaxEligibleLoanAmount(0.0);
            dto.setCollateralRequired(false);
            dto.setReason("Borrower already reached the current active loan limit of " + allowedActiveLoans + ".");
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        // 2) Collateral requirement logic
        boolean poorEarlyHistory =
                totalLoansTaken > 0
                        && totalLoansTaken < 3
                        && (
                        defaultedLoanCount >= totalLoansTaken
                                || totalMissedDays >= 10
                                || maxConsecutiveMissedDays >= 5
                );

        boolean collateralRequired =
                "HIGH".equals(riskCategory)
                        || poorEarlyHistory
                        || riskScore >= manualReviewRiskThreshold;

        // 3) Partial KYC rule:
        // Aadhaar/PAN exists + at least 30% KYC = allow only limited/basic access
        if (!kycVerified) {
            dto.setEligibilityTier("BASIC_KYC");
            dto.setEligibilityStatus(collateralRequired ? "COLLATERAL_REQUIRED" : "ELIGIBLE_WITH_CAUTION");
            dto.setMaxEligibleLoanAmount(collateralRequired ? lowLimit : basicLimit);
            dto.setCollateralRequired(collateralRequired);
            dto.setReason(
                    collateralRequired
                            ? "Basic eligibility unlocked, but collateral is required until KYC verification is completed."
                            : "Basic eligibility unlocked. Complete KYC verification to access higher loan limits."
            );
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        // 4) Fully verified borrower tiering
        if (score >= 80
                && riskScore <= 24
                && monthlyIncome.compareTo(BigDecimal.valueOf(50000)) >= 0
                && incomeProofUploaded) {

            dto.setEligibilityTier("PREMIUM");
            dto.setEligibilityStatus("ELIGIBLE");
            dto.setMaxEligibleLoanAmount(premiumLimit);
            dto.setCollateralRequired(false);
            dto.setReason("Strong profile, low risk, and stable income support premium eligibility.");
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        if (score >= 65
                && riskScore <= 40
                && monthlyIncome.compareTo(BigDecimal.valueOf(30000)) >= 0) {

            dto.setEligibilityTier("STANDARD");
            dto.setEligibilityStatus("ELIGIBLE");
            dto.setMaxEligibleLoanAmount(standardLimit);
            dto.setCollateralRequired(false);
            dto.setReason("Good profile and stable repayment pattern support standard eligibility.");
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        if (score >= 50
                && riskScore <= 59
                && monthlyIncome.compareTo(BigDecimal.valueOf(20000)) >= 0) {

            dto.setEligibilityTier("BASIC");
            dto.setEligibilityStatus("ELIGIBLE");
            dto.setMaxEligibleLoanAmount(basicLimit);
            dto.setCollateralRequired(false);
            dto.setReason("Borrower qualifies for basic eligibility under current score and income.");
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        if (score >= 35) {
            dto.setEligibilityTier("LOW_LIMIT");
            dto.setEligibilityStatus(collateralRequired ? "COLLATERAL_REQUIRED" : "ELIGIBLE_WITH_CAUTION");
            dto.setMaxEligibleLoanAmount(lowLimit);
            dto.setCollateralRequired(collateralRequired);
            dto.setReason(
                    collateralRequired
                            ? "Borrower is eligible only for low-limit loans with collateral or caution."
                            : "Borrower is eligible only for low-limit loans under current profile."
            );
            applyBorrowerOverrideFields(dto, borrower);
            return dto;
        }

        // 5) Final fallback
        dto.setEligibilityTier("HIGH_RISK");
        dto.setEligibilityStatus(collateralProvided ? "PENDING_MANUAL_REVIEW" : "NOT_ELIGIBLE");
        dto.setMaxEligibleLoanAmount(0.0);
        dto.setCollateralRequired(!collateralProvided);
        dto.setReason(
                collateralProvided
                        ? "Borrower is high risk and must be manually reviewed even with collateral."
                        : "Borrower is currently not eligible due to weak score/risk profile."
        );

        applyBorrowerOverrideFields(dto, borrower);
        return dto;
    }

    private void applyBorrowerOverrideFields(EligibilityResultDto dto, Borrower borrower) {
        boolean overrideApplied = false;

        if (!isBlank(borrower.getOverrideEligibilityTier())) {
            dto.setEligibilityTier(borrower.getOverrideEligibilityTier().trim());
            overrideApplied = true;
        }

        if (!isBlank(borrower.getOverrideEligibilityStatus())) {
            dto.setEligibilityStatus(borrower.getOverrideEligibilityStatus().trim());
            overrideApplied = true;
        }

        if (borrower.getOverrideMaxEligibleLoanAmount() != null) {
            dto.setMaxEligibleLoanAmount(borrower.getOverrideMaxEligibleLoanAmount().doubleValue());
            overrideApplied = true;
        }

        if (overrideApplied && !isBlank(borrower.getOverrideReason())) {
            dto.setReason(dto.getReason() + " Admin override applied: " + borrower.getOverrideReason().trim());
        }
    }

    private AdminAccount resolvePolicyAdmin() {
        List<AdminAccount> admins = adminAccountRepository.findAll();

        if (admins.isEmpty()) {
            return null;
        }

        Optional<AdminAccount> activeSuperAdmin = admins.stream()
                .filter(admin -> Boolean.TRUE.equals(admin.getActive()))
                .filter(admin -> Boolean.TRUE.equals(admin.getSuperAdmin()))
                .findFirst();

        if (activeSuperAdmin.isPresent()) {
            return activeSuperAdmin.get();
        }

        Optional<AdminAccount> activeAdmin = admins.stream()
                .filter(admin -> Boolean.TRUE.equals(admin.getActive()))
                .findFirst();

        if (activeAdmin.isPresent()) {
            return activeAdmin.get();
        }

        return admins.stream()
                .min(Comparator.comparing(AdminAccount::getAdminId))
                .orElse(null);
    }

    private int resolveAllowedActiveLoans(Borrower borrower, AdminAccount policyAdmin) {
        if (borrower.getOverrideMaxActiveLoans() != null && borrower.getOverrideMaxActiveLoans() > 0) {
            return borrower.getOverrideMaxActiveLoans();
        }

        if (policyAdmin != null && policyAdmin.getDefaultMaxActiveLoans() != null && policyAdmin.getDefaultMaxActiveLoans() > 0) {
            return policyAdmin.getDefaultMaxActiveLoans();
        }

        return DEFAULT_MAX_ACTIVE_LOANS;
    }

    private double resolvePremiumLimit(Borrower borrower, AdminAccount policyAdmin) {
        if (borrower.getOverrideMaxEligibleLoanAmount() != null) {
            return borrower.getOverrideMaxEligibleLoanAmount().doubleValue();
        }

        if (policyAdmin != null && policyAdmin.getPremiumMaxEligibleLoanAmount() != null) {
            return policyAdmin.getPremiumMaxEligibleLoanAmount().doubleValue();
        }

        return DEFAULT_PREMIUM_LIMIT;
    }

    private double resolveStandardLimit(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getStandardMaxEligibleLoanAmount() != null) {
            return policyAdmin.getStandardMaxEligibleLoanAmount().doubleValue();
        }

        return DEFAULT_STANDARD_LIMIT;
    }

    private double resolveBasicLimit(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getBasicMaxEligibleLoanAmount() != null) {
            return policyAdmin.getBasicMaxEligibleLoanAmount().doubleValue();
        }

        return DEFAULT_BASIC_LIMIT;
    }

    private double resolveLowLimit(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getLowLimitMaxEligibleLoanAmount() != null) {
            return policyAdmin.getLowLimitMaxEligibleLoanAmount().doubleValue();
        }

        return DEFAULT_LOW_LIMIT;
    }

    private int resolveManualReviewRiskThreshold(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getManualReviewRiskThreshold() != null) {
            return policyAdmin.getManualReviewRiskThreshold();
        }

        return DEFAULT_MANUAL_REVIEW_RISK_THRESHOLD;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}