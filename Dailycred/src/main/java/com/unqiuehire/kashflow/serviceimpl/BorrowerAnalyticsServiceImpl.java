package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerAnalyticsSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.EligibilityResultDto;
import com.unqiuehire.kashflow.dto.responsedto.RiskAnalysisResultDto;
import com.unqiuehire.kashflow.entity.AdminAccount;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.Repayment;
import com.unqiuehire.kashflow.repository.AdminAccountRepository;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.repository.RepaymentRepository;
import com.unqiuehire.kashflow.service.BorrowerAnalyticsService;
import com.unqiuehire.kashflow.service.EligibilityService;
import com.unqiuehire.kashflow.service.InternalCreditScoreService;
import com.unqiuehire.kashflow.service.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BorrowerAnalyticsServiceImpl implements BorrowerAnalyticsService {

    private static final int DEFAULT_MAX_ACTIVE_LOANS = 3;

    private final BorrowerRepository borrowerRepository;
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final InternalCreditScoreService internalCreditScoreService;
    private final RiskAnalysisService riskAnalysisService;
    private final EligibilityService eligibilityService;

    @Override
    public ApiResponse<BorrowerAnalyticsSummaryResponseDto> getBorrowerSummary(Long borrowerId) {
        Optional<Borrower> optionalBorrower = borrowerRepository.findById(borrowerId);

        if (optionalBorrower.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower not found", null);
        }

        BorrowerAnalyticsSummaryResponseDto dto = refreshBorrowerDerivedFields(borrowerId);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Borrower analytics summary generated successfully",
                dto
        );
    }

    @Override
    public BorrowerAnalyticsSummaryResponseDto refreshBorrowerDerivedFields(Long borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);
        List<Repayment> repayments = repaymentRepository.findByBorrowerIdOrderByPaymentDateDesc(borrowerId);

        int totalLoansTaken = loans.size();

        int activeLoans = (int) loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .count();

        int closedLoans = (int) loans.stream()
                .filter(loan -> Boolean.TRUE.equals(loan.getIsClosed()))
                .count();

        int defaultedLoanCount = (int) loans.stream()
                .filter(loan -> safeInt(loan.getMissedDaysCount()) >= 10)
                .count();

        int totalRepaymentEvents = repayments.size();

        int totalMissedDays = loans.stream()
                .map(Loan::getMissedDaysCount)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();

        int totalPartialPayments = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPartialPayment()))
                .count();

        int totalAdvancePayments = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsAdvancePayment()))
                .count();

        int totalLatePayments = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsLatePayment()))
                .count();

        int totalPreClosures = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPreClosure()))
                .count();

        int maxConsecutiveMissedDays = loans.stream()
                .map(Loan::getConsecutiveMissedDays)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        double averageMissedDaysPerLoan = totalLoansTaken == 0
                ? 0.0
                : (double) totalMissedDays / totalLoansTaken;

        double totalDisbursedAmount = loans.stream()
                .map(Loan::getDisbursedAmount)
                .filter(v -> v != null)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        double totalRepaidAmount = repayments.stream()
                .map(Repayment::getAmountPaid)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        double currentOutstandingAmount = loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .map(Loan::getRemainingAmount)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        int internalCreditScore = internalCreditScoreService.calculateInternalCreditScore(borrower, loans, repayments);
        RiskAnalysisResultDto riskResult = riskAnalysisService.analyze(borrower, loans, repayments);
        EligibilityResultDto eligibilityResult = eligibilityService.evaluate(borrower, internalCreditScore, riskResult, loans);

        syncDerivedValuesToBorrower(
                borrower,
                totalLoansTaken,
                activeLoans,
                closedLoans,
                defaultedLoanCount,
                totalMissedDays,
                totalPartialPayments,
                totalAdvancePayments,
                totalLatePayments,
                maxConsecutiveMissedDays,
                currentOutstandingAmount,
                internalCreditScore,
                riskResult,
                eligibilityResult,
                loans
        );

        Borrower savedBorrower = borrowerRepository.save(borrower);

        return buildSummaryDto(
                savedBorrower,
                totalRepaymentEvents,
                totalPreClosures,
                averageMissedDaysPerLoan,
                totalDisbursedAmount,
                totalRepaidAmount
        );
    }

    private void syncDerivedValuesToBorrower(
            Borrower borrower,
            int totalLoansTaken,
            int activeLoans,
            int closedLoans,
            int defaultedLoanCount,
            int totalMissedDays,
            int totalPartialPayments,
            int totalAdvancePayments,
            int totalLatePayments,
            int maxConsecutiveMissedDays,
            double currentOutstandingAmount,
            int internalCreditScore,
            RiskAnalysisResultDto riskResult,
            EligibilityResultDto eligibilityResult,
            List<Loan> loans
    ) {
        borrower.setTotalLoansTaken(totalLoansTaken);
        borrower.setActiveLoanCount(activeLoans);
        borrower.setCurrentOutstandingAmount(BigDecimal.valueOf(round2(currentOutstandingAmount)));
        borrower.setDefaultedLoanCount(defaultedLoanCount);
        borrower.setLoansClosedSuccessfully(closedLoans);
        borrower.setLoansClosedEarly(
                (int) loans.stream().filter(loan -> Boolean.TRUE.equals(loan.getClosedEarly())).count()
        );
        borrower.setTotalMissedDays(totalMissedDays);
        borrower.setTotalPartialDays(totalPartialPayments);
        borrower.setTotalAdvanceDays(totalAdvancePayments);
        borrower.setTotalLatePayments(totalLatePayments);
        borrower.setMaxConsecutiveMissedDays(maxConsecutiveMissedDays);

        borrower.setInternalCreditScore(internalCreditScore);
        borrower.setCibil(internalCreditScore); // compatibility mirror only

        borrower.setRiskScore(riskResult == null ? 0 : safeInt(riskResult.getRiskScore()));
        borrower.setRiskCategory(riskResult == null || isBlank(riskResult.getRiskCategory())
                ? "UNKNOWN"
                : riskResult.getRiskCategory().trim().toUpperCase());

        borrower.setEligibilityTier(eligibilityResult == null || isBlank(eligibilityResult.getEligibilityTier())
                ? "UNASSIGNED"
                : eligibilityResult.getEligibilityTier());

        borrower.setEligibilityStatus(eligibilityResult == null || isBlank(eligibilityResult.getEligibilityStatus())
                ? "PENDING_REVIEW"
                : eligibilityResult.getEligibilityStatus());

        double maxEligibleLoanAmount = eligibilityResult == null ? 0.0 : safeDouble(eligibilityResult.getMaxEligibleLoanAmount());
        borrower.setMaxEligibleLoanAmount(BigDecimal.valueOf(round2(maxEligibleLoanAmount)));

        borrower.setEligibilityScore(Math.max(
                0,
                internalCreditScore - safeInt(riskResult == null ? null : riskResult.getRiskScore())
        ));
    }

    private BorrowerAnalyticsSummaryResponseDto buildSummaryDto(
            Borrower borrower,
            int totalRepaymentEvents,
            int totalPreClosures,
            double averageMissedDaysPerLoan,
            double totalDisbursedAmount,
            double totalRepaidAmount
    ) {
        BorrowerAnalyticsSummaryResponseDto dto = new BorrowerAnalyticsSummaryResponseDto();

        int activeLoans = safeInt(borrower.getActiveLoanCount());
        int allowedActiveLoanLimit = resolveAllowedActiveLoans(borrower);
        int remainingActiveLoanSlots = Math.max(0, allowedActiveLoanLimit - activeLoans);

        dto.setBorrowerId(borrower.getBorrowerId());
        dto.setBorrowerName(borrower.getBorrowerName());

        dto.setCurrentCibil(safeInt(borrower.getCibil()));
        dto.setInternalCreditScore(safeInt(borrower.getInternalCreditScore()));
        dto.setRiskScore(safeInt(borrower.getRiskScore()));
        dto.setRiskCategory(defaultString(borrower.getRiskCategory(), "UNKNOWN"));

        dto.setEligibilityTier(defaultString(borrower.getEligibilityTier(), "UNASSIGNED"));
        dto.setEligibilityStatus(defaultString(borrower.getEligibilityStatus(), "PENDING_REVIEW"));
        dto.setMaxEligibleLoanAmount(safeBigDecimalToDouble(borrower.getMaxEligibleLoanAmount()));
        dto.setCollateralRequired("COLLATERAL_REQUIRED".equalsIgnoreCase(borrower.getEligibilityStatus()));
        dto.setRecommendation(buildRecommendation(borrower, allowedActiveLoanLimit, remainingActiveLoanSlots));

        dto.setAllowedActiveLoanLimit(allowedActiveLoanLimit);
        dto.setRemainingActiveLoanSlots(remainingActiveLoanSlots);

        dto.setTotalLoansTaken(safeInt(borrower.getTotalLoansTaken()));
        dto.setActiveLoans(activeLoans);
        dto.setClosedLoans(safeInt(borrower.getLoansClosedSuccessfully()));
        dto.setDefaultedLoanCount(safeInt(borrower.getDefaultedLoanCount()));

        dto.setTotalRepaymentEvents(totalRepaymentEvents);
        dto.setTotalMissedDays(safeInt(borrower.getTotalMissedDays()));
        dto.setTotalPartialPayments(safeInt(borrower.getTotalPartialDays()));
        dto.setTotalAdvancePayments(safeInt(borrower.getTotalAdvanceDays()));
        dto.setTotalLatePayments(safeInt(borrower.getTotalLatePayments()));
        dto.setTotalPreClosures(totalPreClosures);
        dto.setMaxConsecutiveMissedDays(safeInt(borrower.getMaxConsecutiveMissedDays()));

        dto.setAverageMissedDaysPerLoan(round2(averageMissedDaysPerLoan));
        dto.setTotalDisbursedAmount(round2(totalDisbursedAmount));
        dto.setTotalRepaidAmount(round2(totalRepaidAmount));
        dto.setCurrentOutstandingAmount(safeBigDecimalToDouble(borrower.getCurrentOutstandingAmount()));

        return dto;
    }

    private int resolveAllowedActiveLoans(Borrower borrower) {
        if (borrower.getOverrideMaxActiveLoans() != null && borrower.getOverrideMaxActiveLoans() > 0) {
            return borrower.getOverrideMaxActiveLoans();
        }

        AdminAccount policyAdmin = resolvePolicyAdmin();

        if (policyAdmin != null
                && policyAdmin.getDefaultMaxActiveLoans() != null
                && policyAdmin.getDefaultMaxActiveLoans() > 0) {
            return policyAdmin.getDefaultMaxActiveLoans();
        }

        return DEFAULT_MAX_ACTIVE_LOANS;
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

    private String buildRecommendation(Borrower borrower, int allowedActiveLoanLimit, int remainingActiveLoanSlots) {
        String eligibilityStatus = defaultString(borrower.getEligibilityStatus(), "PENDING_REVIEW");
        String riskCategory = defaultString(borrower.getRiskCategory(), "UNKNOWN");
        Double maxEligible = safeBigDecimalToDouble(borrower.getMaxEligibleLoanAmount());

        if ("NOT_ELIGIBLE".equalsIgnoreCase(eligibilityStatus)) {
            return "Borrower is currently not eligible for new loan applications.";
        }

        if ("ACTIVE_LOAN_LIMIT".equalsIgnoreCase(defaultString(borrower.getEligibilityTier(), ""))) {
            return "Borrower has reached the active loan cap of " + allowedActiveLoanLimit + ".";
        }

        if ("COLLATERAL_REQUIRED".equalsIgnoreCase(eligibilityStatus)) {
            return "Borrower can proceed only with collateral-backed applications.";
        }

        if ("PENDING_REVIEW".equalsIgnoreCase(eligibilityStatus)) {
            return "Borrower profile needs review or completion before loan access.";
        }

        if ("LOW".equalsIgnoreCase(riskCategory)) {
            return "Low-risk borrower. Eligible up to " + round2(maxEligible)
                    + " with " + remainingActiveLoanSlots + " loan slot(s) remaining.";
        }

        if ("MEDIUM".equalsIgnoreCase(riskCategory)) {
            return "Moderate-risk borrower. Eligible within the current cap, with "
                    + remainingActiveLoanSlots + " loan slot(s) remaining.";
        }

        if ("HIGH".equalsIgnoreCase(riskCategory)) {
            return "High-risk borrower. Only restricted or collateral-backed lending should be considered.";
        }

        return "Borrower summary refreshed successfully.";
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double safeBigDecimalToDouble(BigDecimal value) {
        return value == null ? 0.0 : round2(value.doubleValue());
    }

    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}