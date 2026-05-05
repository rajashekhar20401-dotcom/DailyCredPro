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
        int activeLoans = (int) loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .count();

        int closedLoans = (int) loans.stream()
                .filter(loan -> Boolean.TRUE.equals(loan.getIsClosed()))
                .count();

        int defaultedLoans = (int) loans.stream()
                .filter(loan -> safeInt(loan.getMissedDaysCount()) >= 10)
                .count();

        double totalPrincipalDisbursed = loans.stream()
                .map(Loan::getDisbursedAmount)
                .filter(Objects::nonNull)
                .mapToDouble(value -> value.doubleValue())
                .sum();

        double expectedTotalRepayment = loans.stream()
                .filter(loan -> loan.getTotalRepayableAmount() != null)
                .mapToDouble(loan -> loan.getTotalRepayableAmount().doubleValue())
                .sum();

        if (expectedTotalRepayment == 0) {
            expectedTotalRepayment = loans.stream()
                    .map(Loan::getSanctionedAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }

        double totalCollected = repayments.stream()
                .map(Repayment::getAmountPaid)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double currentOutstanding = loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .map(Loan::getRemainingAmount)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double totalOverdue = loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .map(Loan::getOverdueAmount)
                .filter(Objects::nonNull)
                .mapToDouble(value -> value.doubleValue())
                .sum();

        double totalPenaltyAccrued = loans.stream()
                .map(Loan::getTotalPenaltyAmount)
                .filter(Objects::nonNull)
                .mapToDouble(value -> value.doubleValue())
                .sum();

        double totalPenaltyCollected = repayments.stream()
                .map(Repayment::getAllocatedToPenalty)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double estimatedGrossProfit = loans.stream()
                .filter(loan -> loan.getTotalRepayableAmount() != null && loan.getDisbursedAmount() != null)
                .mapToDouble(loan -> loan.getTotalRepayableAmount().doubleValue() - loan.getDisbursedAmount().doubleValue())
                .sum();

        double estimatedPlatformFee = loans.stream()
                .map(Loan::getPlatformFeeAmount)
                .filter(Objects::nonNull)
                .mapToDouble(value -> value.doubleValue())
                .sum();

        double estimatedNetProfit = estimatedGrossProfit - estimatedPlatformFee;
        if (estimatedNetProfit < 0) {
            estimatedNetProfit = 0;
        }

        LocalDate today = LocalDate.now();

        int paidTodayCount = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()))
                .filter(r -> r.getPaymentStatus() == PaymentStatus.FULL || r.getPaymentStatus() == PaymentStatus.LATE_FULL)
                .count();

        int missedTodayCount = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()) && r.getPaymentStatus() == PaymentStatus.MISSED)
                .count();

        int partialTodayCount = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()) && r.getPaymentStatus() == PaymentStatus.PARTIAL)
                .count();

        int advanceTodayCount = (int) repayments.stream()
                .filter(r -> today.equals(r.getPaymentDate()))
                .filter(r -> r.getPaymentStatus() == PaymentStatus.ADVANCE || r.getPaymentStatus() == PaymentStatus.PRE_CLOSURE)
                .count();

        int currentlyOverdueBorrowerCount = (int) loans.stream()
                .filter(loan -> loan.getOverdueAmount() != null && loan.getOverdueAmount().doubleValue() > 0)
                .map(Loan::getBorrowerId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        double projectedCollectionRate = expectedTotalRepayment > 0
                ? (totalCollected / expectedTotalRepayment) * 100
                : 0.0;

        double projectedRecoveryRate = totalPrincipalDisbursed > 0
                ? (totalCollected / totalPrincipalDisbursed) * 100
                : 0.0;

        double averageExpectedReturnPerLoan = totalLoans > 0
                ? estimatedNetProfit / totalLoans
                : 0.0;

        LenderDashboardSummaryResponseDto dto = new LenderDashboardSummaryResponseDto();
        dto.setLenderId(lenderId);

        dto.setTotalLoans(totalLoans);
        dto.setActiveLoans(activeLoans);
        dto.setClosedLoans(closedLoans);
        dto.setDefaultedLoans(defaultedLoans);

        dto.setTotalPrincipalDisbursed(round2(totalPrincipalDisbursed));
        dto.setExpectedTotalRepayment(round2(expectedTotalRepayment));
        dto.setTotalCollected(round2(totalCollected));

        dto.setCurrentOutstanding(round2(currentOutstanding));
        dto.setTotalOutstanding(round2(currentOutstanding));
        dto.setTotalOverdue(round2(totalOverdue));
        dto.setCurrentOutstandingExposure(round2(currentOutstanding));
        dto.setRecoverableAmount(round2(currentOutstanding));

        dto.setTotalPenaltyAccrued(round2(totalPenaltyAccrued));
        dto.setTotalPenaltyCollected(round2(totalPenaltyCollected));

        dto.setEstimatedGrossProfit(round2(estimatedGrossProfit));
        dto.setProjectedProfit(round2(estimatedGrossProfit));
        dto.setEstimatedPlatformFee(round2(estimatedPlatformFee));
        dto.setEstimatedNetProfit(round2(estimatedNetProfit));
        dto.setCapitalReadyForReuse(round2(totalCollected));

        dto.setPaidTodayCount(paidTodayCount);
        dto.setPaidToday(paidTodayCount);
        dto.setMissedTodayCount(missedTodayCount);
        dto.setMissedToday(missedTodayCount);
        dto.setPartialTodayCount(partialTodayCount);
        dto.setPartialToday(partialTodayCount);
        dto.setAdvanceTodayCount(advanceTodayCount);
        dto.setAdvanceToday(advanceTodayCount);

        dto.setCurrentlyOverdueBorrowerCount(currentlyOverdueBorrowerCount);
        dto.setOverdueBorrowers(currentlyOverdueBorrowerCount);

        dto.setProjectedCollectionRate(round2(projectedCollectionRate));
        dto.setProjectedRecoveryRate(round2(projectedRecoveryRate));
        dto.setAverageExpectedReturnPerLoan(round2(averageExpectedReturnPerLoan));

        return dto;
    }

    @Override
    public List<LenderTodayCollectionItemResponseDto> getTodayCollections(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        Set<Long> lenderLoanIds = loans.stream()
                .map(Loan::getLoanId)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();

        return getRepaymentsForLoans(loans).stream()
                .filter(r -> today.equals(r.getPaymentDate()))
                .filter(r -> r.getLoan() != null && lenderLoanIds.contains(r.getLoan().getLoanId()))
                .map(r -> {
                    LenderTodayCollectionItemResponseDto dto = new LenderTodayCollectionItemResponseDto();
                    dto.setBorrowerId(r.getBorrowerId());
                    dto.setBorrowerName(
                            borrowerRepository.findById(r.getBorrowerId())
                                    .map(Borrower::getBorrowerName)
                                    .orElse("Unknown Borrower")
                    );
                    dto.setLoanId(r.getLoan().getLoanId());
                    dto.setAmountPaid(round2(safeDouble(r.getAmountPaid())));
                    dto.setPaymentMode(r.getPaymentMode());
                    dto.setPaymentStatus(r.getPaymentStatus());
                    dto.setPaymentDate(r.getPaymentDate());
                    dto.setBalanceAmount(round2(safeDouble(r.getBalanceAmount())));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanPlanPerformanceResponseDto> getLoanPlanPerformance(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        Map<Long, List<Loan>> byPlan = loans.stream()
                .filter(loan -> loan.getPlanId() != null)
                .collect(Collectors.groupingBy(Loan::getPlanId));

        List<LoanPlanPerformanceResponseDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<Loan>> entry : byPlan.entrySet()) {
            Long planId = entry.getKey();
            List<Loan> planLoans = entry.getValue();

            String planName = loanPlanRepository.findById(planId)
                    .map(LoanPlan::getPlanName)
                    .orElse("Unknown Plan");

            List<Repayment> repayments = getRepaymentsForLoans(planLoans);

            double totalDisbursed = planLoans.stream()
                    .map(Loan::getDisbursedAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(value -> value.doubleValue())
                    .sum();

            double totalCollected = repayments.stream()
                    .map(Repayment::getAmountPaid)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            double totalOutstanding = planLoans.stream()
                    .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                    .map(Loan::getRemainingAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            double projectedProfit = planLoans.stream()
                    .filter(loan -> loan.getTotalRepayableAmount() != null && loan.getDisbursedAmount() != null)
                    .mapToDouble(loan -> loan.getTotalRepayableAmount().doubleValue() - loan.getDisbursedAmount().doubleValue())
                    .sum();

            LoanPlanPerformanceResponseDto dto = new LoanPlanPerformanceResponseDto();
            dto.setPlanId(planId);
            dto.setPlanName(planName);
            dto.setTotalLoans(planLoans.size());
            dto.setActiveLoans((int) planLoans.stream().filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed())).count());
            dto.setClosedLoans((int) planLoans.stream().filter(loan -> Boolean.TRUE.equals(loan.getIsClosed())).count());
            dto.setTotalDisbursed(round2(totalDisbursed));
            dto.setTotalCollected(round2(totalCollected));
            dto.setTotalOutstanding(round2(totalOutstanding));
            dto.setProjectedProfit(round2(projectedProfit));

            result.add(dto);
        }

        result.sort(Comparator.comparing(LoanPlanPerformanceResponseDto::getPlanName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    @Override
    public List<BorrowerRiskBreakdownResponseDto> getBorrowerRiskBreakdown(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        Map<Long, List<Loan>> loansByBorrower = loans.stream()
                .filter(loan -> loan.getBorrowerId() != null)
                .collect(Collectors.groupingBy(Loan::getBorrowerId));

        List<BorrowerRiskBreakdownResponseDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<Loan>> entry : loansByBorrower.entrySet()) {
            Long borrowerId = entry.getKey();
            List<Loan> borrowerLoans = entry.getValue();
            List<Repayment> repayments = getRepaymentsForLoans(borrowerLoans);

            Borrower borrower = borrowerRepository.findById(borrowerId).orElse(null);

            double currentOutstanding = borrowerLoans.stream()
                    .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                    .map(Loan::getRemainingAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            double totalOverdueAmount = borrowerLoans.stream()
                    .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                    .map(Loan::getOverdueAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(value -> value.doubleValue())
                    .sum();

            int totalMissedDays = borrowerLoans.stream()
                    .map(Loan::getMissedDaysCount)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            int maxConsecutiveMissedDays = borrowerLoans.stream()
                    .map(Loan::getConsecutiveMissedDays)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);

            int totalLatePayments = (int) repayments.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsLatePayment()))
                    .count();

            int activeLoanCount = (int) borrowerLoans.stream()
                    .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                    .count();

            BorrowerRiskBreakdownResponseDto dto = new BorrowerRiskBreakdownResponseDto();
            dto.setBorrowerId(borrowerId);
            dto.setBorrowerName(borrower == null ? "Unknown Borrower" : borrower.getBorrowerName());
            dto.setPhoneNumber(borrower == null ? null : borrower.getPhoneNumber());
            dto.setInternalCreditScore(borrower == null ? 0 : safeInt(borrower.getInternalCreditScore()));
            dto.setRiskScore(borrower == null ? 0 : safeInt(borrower.getRiskScore()));
            dto.setRiskCategory(borrower == null ? "UNASSESSED" : defaultString(borrower.getRiskCategory(), "UNASSESSED"));
            dto.setTotalLoansWithLender(borrowerLoans.size());
            dto.setActiveLoanCount(activeLoanCount);
            dto.setCurrentOutstanding(round2(currentOutstanding));
            dto.setTotalOverdueAmount(round2(totalOverdueAmount));
            dto.setTotalMissedDays(totalMissedDays);
            dto.setMaxConsecutiveMissedDays(maxConsecutiveMissedDays);
            dto.setTotalLatePayments(totalLatePayments);

            result.add(dto);
        }

        result.sort(
                Comparator.comparing(BorrowerRiskBreakdownResponseDto::getCurrentOutstanding, Comparator.nullsFirst(Double::compareTo))
                        .thenComparing(BorrowerRiskBreakdownResponseDto::getTotalMissedDays, Comparator.nullsFirst(Integer::compareTo))
                        .reversed()
        );

        return result;
    }

    private List<Repayment> getRepaymentsForLoans(List<Loan> loans) {
        List<Repayment> all = new ArrayList<>();
        for (Loan loan : loans) {
            all.addAll(repaymentRepository.findByLoanLoanId(loan.getLoanId()));
        }
        return all;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}