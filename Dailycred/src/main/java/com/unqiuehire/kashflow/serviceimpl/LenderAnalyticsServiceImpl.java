package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.PaymentStatus;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerRiskBreakdownResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LenderDashboardSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LenderTodayCollectionItemResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LoanPlanPerformanceResponseDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.LoanPlan;
import com.unqiuehire.kashflow.entity.Repayment;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LoanPlanRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.repository.RepaymentRepository;
import com.unqiuehire.kashflow.service.LenderAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LenderAnalyticsServiceImpl implements LenderAnalyticsService {

    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final LoanPlanRepository loanPlanRepository;
    private final BorrowerRepository borrowerRepository;

    @Override
    public LenderDashboardSummaryResponseDto getDashboardSummary(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        List<Repayment> repayments = getRepaymentsForLoans(loans);

        int totalLoans = loans.size();
        int activeLoans = (int) loans.stream().filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed())).count();
        int closedLoans = (int) loans.stream().filter(loan -> Boolean.TRUE.equals(loan.getIsClosed())).count();

        double totalPrincipalDisbursed = loans.stream()
                .map(Loan::getDisbursedAmount)
                .filter(Objects::nonNull)
                .mapToDouble(v -> v.doubleValue())
                .sum();

        double totalCollected = repayments.stream()
                .map(Repayment::getAmountPaid)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double totalOutstanding = loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .map(Loan::getRemainingAmount)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double totalPenaltyCollected = repayments.stream()
                .map(Repayment::getAllocatedToPenalty)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double projectedProfit = loans.stream()
                .filter(loan -> loan.getTotalRepayableAmount() != null && loan.getDisbursedAmount() != null)
                .mapToDouble(loan -> loan.getTotalRepayableAmount().doubleValue() - loan.getDisbursedAmount().doubleValue())
                .sum();

        LocalDate today = LocalDate.now();

        int paidToday = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()) && r.getPaymentStatus() == PaymentStatus.FULL)
                .count();

        int missedToday = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()) && r.getPaymentStatus() == PaymentStatus.MISSED)
                .count();

        int partialToday = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()) && r.getPaymentStatus() == PaymentStatus.PARTIAL)
                .count();

        int advanceToday = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()) && r.getPaymentStatus() == PaymentStatus.ADVANCE)
                .count();

        int overdueBorrowers = (int) loans.stream()
                .filter(loan -> loan.getOverdueAmount() != null && loan.getOverdueAmount().doubleValue() > 0)
                .map(Loan::getBorrowerId)
                .distinct()
                .count();

        LenderDashboardSummaryResponseDto dto = new LenderDashboardSummaryResponseDto();
        dto.setLenderId(lenderId);
        dto.setTotalLoans(totalLoans);
        dto.setActiveLoans(activeLoans);
        dto.setClosedLoans(closedLoans);
        dto.setTotalPrincipalDisbursed(totalPrincipalDisbursed);
        dto.setTotalCollected(totalCollected);
        dto.setTotalOutstanding(totalOutstanding);
        dto.setTotalPenaltyCollected(totalPenaltyCollected);
        dto.setProjectedProfit(projectedProfit);
        dto.setCurrentOutstandingExposure(totalOutstanding);
        dto.setRecoverableAmount(totalOutstanding);
        dto.setCapitalReadyForReuse(totalCollected);
        dto.setPaidToday(paidToday);
        dto.setMissedToday(missedToday);
        dto.setPartialToday(partialToday);
        dto.setAdvanceToday(advanceToday);
        dto.setOverdueBorrowers(overdueBorrowers);

        return dto;
    }

    @Override
    public List<LenderTodayCollectionItemResponseDto> getTodayCollections(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        Set<Long> lenderLoanIds = loans.stream().map(Loan::getLoanId).collect(Collectors.toSet());
        LocalDate today = LocalDate.now();

        return getRepaymentsForLoans(loans).stream()
                .filter(r -> today.equals(r.getPaymentDate()))
                .filter(r -> lenderLoanIds.contains(r.getLoan().getLoanId()))
                .map(r -> {
                    LenderTodayCollectionItemResponseDto dto = new LenderTodayCollectionItemResponseDto();
                    dto.setBorrowerId(r.getBorrowerId());
                    dto.setBorrowerName(
                            borrowerRepository.findById(r.getBorrowerId())
                                    .map(Borrower::getBorrowerName)
                                    .orElse("Unknown Borrower")
                    );
                    dto.setLoanId(r.getLoan().getLoanId());
                    dto.setAmountPaid(r.getAmountPaid());
                    dto.setPaymentMode(r.getPaymentMode());
                    dto.setPaymentStatus(r.getPaymentStatus());
                    dto.setPaymentDate(r.getPaymentDate());
                    dto.setBalanceAmount(r.getBalanceAmount());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanPlanPerformanceResponseDto> getLoanPlanPerformance(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        Map<Long, List<Loan>> byPlan = loans.stream().collect(Collectors.groupingBy(Loan::getPlanId));

        List<LoanPlanPerformanceResponseDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<Loan>> entry : byPlan.entrySet()) {
            Long planId = entry.getKey();
            List<Loan> planLoans = entry.getValue();

            String planName = loanPlanRepository.findById(planId)
                    .map(LoanPlan::getPlanName)
                    .orElse("Unknown Plan");

            List<Repayment> repayments = getRepaymentsForLoans(planLoans);

            LoanPlanPerformanceResponseDto dto = new LoanPlanPerformanceResponseDto();
            dto.setPlanId(planId);
            dto.setPlanName(planName);
            dto.setTotalLoans(planLoans.size());
            dto.setActiveLoans((int) planLoans.stream().filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed())).count());
            dto.setClosedLoans((int) planLoans.stream().filter(loan -> Boolean.TRUE.equals(loan.getIsClosed())).count());
            dto.setTotalDisbursed(planLoans.stream()
                    .map(Loan::getDisbursedAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(v -> v.doubleValue())
                    .sum());
            dto.setTotalCollected(repayments.stream()
                    .map(Repayment::getAmountPaid)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum());
            dto.setTotalOutstanding(planLoans.stream()
                    .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                    .map(Loan::getRemainingAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum());
            dto.setProjectedProfit(planLoans.stream()
                    .filter(loan -> loan.getTotalRepayableAmount() != null && loan.getDisbursedAmount() != null)
                    .mapToDouble(loan -> loan.getTotalRepayableAmount().doubleValue() - loan.getDisbursedAmount().doubleValue())
                    .sum());

            result.add(dto);
        }

        return result;
    }

    @Override
    public List<BorrowerRiskBreakdownResponseDto> getBorrowerRiskBreakdown(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        Map<Long, List<Loan>> loansByBorrower = loans.stream().collect(Collectors.groupingBy(Loan::getBorrowerId));

        List<BorrowerRiskBreakdownResponseDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<Loan>> entry : loansByBorrower.entrySet()) {
            Long borrowerId = entry.getKey();
            List<Loan> borrowerLoans = entry.getValue();
            List<Repayment> repayments = getRepaymentsForLoans(borrowerLoans);

            Borrower borrower = borrowerRepository.findById(borrowerId).orElse(null);

            BorrowerRiskBreakdownResponseDto dto = new BorrowerRiskBreakdownResponseDto();
            dto.setBorrowerId(borrowerId);
            dto.setBorrowerName(borrower == null ? "Unknown Borrower" : borrower.getBorrowerName());
            dto.setInternalCreditScore(borrower == null ? 0 : borrower.getInternalCreditScore());
            dto.setRiskScore(borrower == null ? 0 : borrower.getRiskScore());
            dto.setRiskCategory(borrower == null ? "UNASSESSED" : borrower.getRiskCategory());
            dto.setTotalLoansWithLender(borrowerLoans.size());
            dto.setCurrentOutstanding(borrowerLoans.stream()
                    .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                    .map(Loan::getRemainingAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum());
            dto.setTotalMissedDays(borrowerLoans.stream()
                    .map(Loan::getMissedDaysCount)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum());
            dto.setTotalLatePayments((int) repayments.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsLatePayment()))
                    .count());

            result.add(dto);
        }

        return result;
    }

    private List<Repayment> getRepaymentsForLoans(List<Loan> loans) {
        List<Repayment> all = new ArrayList<>();
        for (Loan loan : loans) {
            all.addAll(repaymentRepository.findByLoanLoanId(loan.getLoanId()));
        }
        return all;
    }
}
