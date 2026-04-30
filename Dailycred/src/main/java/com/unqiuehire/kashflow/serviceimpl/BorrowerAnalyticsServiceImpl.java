package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.dto.responsedto.*;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.Repayment;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BorrowerAnalyticsServiceImpl implements BorrowerAnalyticsService {

    private final BorrowerRepository borrowerRepository;
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final InternalCreditScoreService internalCreditScoreService;
    private final RiskAnalysisService riskAnalysisService;
    private final EligibilityService eligibilityService;

    @Override
    public ApiResponse<BorrowerAnalyticsSummaryResponseDto> getBorrowerSummary(Long borrowerId) {
        Optional<Borrower> optionalBorrower = borrowerRepository.findById(borrowerId);

        if (optionalBorrower.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower not found", null);
        }

        Borrower borrower = optionalBorrower.get();
        List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);
        List<Repayment> repayments = repaymentRepository.findByBorrowerIdOrderByPaymentDateDesc(borrowerId);

        int totalLoansTaken = loans.size();
        int activeLoans = (int) loans.stream().filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed())).count();
        int closedLoans = (int) loans.stream().filter(loan -> Boolean.TRUE.equals(loan.getIsClosed())).count();
        int defaultedLoanCount = (int) loans.stream()
                .filter(loan -> loan.getMissedDaysCount() != null && loan.getMissedDaysCount() >= 10)
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

        int maxConsecutiveMissedDays = repayments.stream()
                .map(Repayment::getMissedDays)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        double averageMissedDaysPerLoan = totalLoansTaken == 0 ? 0.0 : (double) totalMissedDays / totalLoansTaken;

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

        // sync derived values back into borrower
        borrower.setTotalLoansTaken(totalLoansTaken);
        borrower.setActiveLoanCount(activeLoans);
        borrower.setCurrentOutstandingAmount(BigDecimal.valueOf(currentOutstandingAmount));
        borrower.setDefaultedLoanCount(defaultedLoanCount);
        borrower.setLoansClosedSuccessfully(closedLoans);
        borrower.setLoansClosedEarly((int) loans.stream().filter(loan -> Boolean.TRUE.equals(loan.getClosedEarly())).count());
        borrower.setTotalMissedDays(totalMissedDays);
        borrower.setTotalPartialDays(totalPartialPayments);
        borrower.setTotalAdvanceDays(totalAdvancePayments);
        borrower.setTotalLatePayments(totalLatePayments);
        borrower.setMaxConsecutiveMissedDays(maxConsecutiveMissedDays);
        borrower.setInternalCreditScore(internalCreditScore);
        borrower.setRiskScore(riskResult.getRiskScore());
        borrower.setRiskCategory(riskResult.getRiskCategory());
        borrower.setEligibilityTier(eligibilityResult.getEligibilityTier());
        borrower.setEligibilityStatus(eligibilityResult.getEligibilityStatus());
        borrower.setMaxEligibleLoanAmount(BigDecimal.valueOf(eligibilityResult.getMaxEligibleLoanAmount()));
        borrower.setEligibilityScore(Math.max(0, internalCreditScore - riskResult.getRiskScore()));

        borrowerRepository.save(borrower);

        BorrowerAnalyticsSummaryResponseDto dto = new BorrowerAnalyticsSummaryResponseDto();
        dto.setBorrowerId(borrower.getBorrowerId());
        dto.setBorrowerName(borrower.getBorrowerName());
        dto.setCurrentCibil(internalCreditScore);
        dto.setInternalCreditScore(internalCreditScore);
        dto.setRiskScore(riskResult.getRiskScore());
        dto.setRiskCategory(riskResult.getRiskCategory());
        dto.setEligibilityTier(eligibilityResult.getEligibilityTier());
        dto.setEligibilityStatus(eligibilityResult.getEligibilityStatus());
        dto.setMaxEligibleLoanAmount(eligibilityResult.getMaxEligibleLoanAmount());
        dto.setCollateralRequired(eligibilityResult.getCollateralRequired());
        dto.setRecommendation(eligibilityResult.getReason());

        dto.setTotalLoansTaken(totalLoansTaken);
        dto.setActiveLoans(activeLoans);
        dto.setClosedLoans(closedLoans);
        dto.setDefaultedLoanCount(defaultedLoanCount);
        dto.setTotalRepaymentEvents(totalRepaymentEvents);
        dto.setTotalMissedDays(totalMissedDays);
        dto.setTotalPartialPayments(totalPartialPayments);
        dto.setTotalAdvancePayments(totalAdvancePayments);
        dto.setTotalLatePayments(totalLatePayments);
        dto.setTotalPreClosures(totalPreClosures);
        dto.setMaxConsecutiveMissedDays(maxConsecutiveMissedDays);
        dto.setAverageMissedDaysPerLoan(averageMissedDaysPerLoan);
        dto.setTotalDisbursedAmount(totalDisbursedAmount);
        dto.setTotalRepaidAmount(totalRepaidAmount);
        dto.setCurrentOutstandingAmount(currentOutstandingAmount);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower analytics summary generated successfully", dto);
    }
}