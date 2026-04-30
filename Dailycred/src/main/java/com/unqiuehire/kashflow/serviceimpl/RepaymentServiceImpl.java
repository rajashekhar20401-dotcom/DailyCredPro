package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApplicationStatus;
import com.unqiuehire.kashflow.constant.PaymentMode;
import com.unqiuehire.kashflow.constant.PaymentStatus;
import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.constant.WalletTransactionType;
import com.unqiuehire.kashflow.dto.requestdto.RepaymentRequestDTO;
import com.unqiuehire.kashflow.dto.responsedto.RepaymentResponseDTO;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.LoanApplication;
import com.unqiuehire.kashflow.entity.Repayment;
import com.unqiuehire.kashflow.repository.LoanApplicationRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.repository.RepaymentRepository;
import com.unqiuehire.kashflow.service.RepaymentService;
import com.unqiuehire.kashflow.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepaymentServiceImpl implements RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public RepaymentResponseDTO makePayment(RepaymentRequestDTO request) {

        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        LoanApplication loanApplication = loanApplicationRepository.findById(request.getLoanApplicationId())
                .orElseThrow(() -> new RuntimeException("Loan Application not found"));

        if (loanApplication.getStatus() != ApplicationStatus.APPROVED) {
            throw new RuntimeException("Loan not approved yet");
        }

        if (!loan.getLoanApplicationId().equals(request.getLoanApplicationId())) {
            throw new RuntimeException("Loan and Loan Application mismatch");
        }

        if (Boolean.TRUE.equals(loan.getIsClosed())) {
            throw new RuntimeException("Loan already closed");
        }

        LocalDate paymentDate = request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate();
        PaymentMode paymentMode = request.getPaymentMode() == null ? PaymentMode.CASH : request.getPaymentMode();

        BigDecimal amountPaid = BigDecimal.valueOf(request.getAmountPaid() == null ? 0.0 : request.getAmountPaid());
        BigDecimal dailyDue = BigDecimal.valueOf(loan.getDailyEmi() == null ? 0.0 : loan.getDailyEmi());

        BigDecimal remainingAmount = BigDecimal.valueOf(
                loan.getRemainingAmount() == null ? 0.0 : loan.getRemainingAmount()
        );

        BigDecimal penaltyAmount = BigDecimal.valueOf(
                loan.getPenaltyAmount() == null ? 0.0 : loan.getPenaltyAmount()
        );

        BigDecimal overdueAmount = loan.getOverdueAmount() == null
                ? BigDecimal.ZERO
                : loan.getOverdueAmount();

        LocalDate nextDueDate = loan.getNextDueDate() == null ? loan.getStartDate() : loan.getNextDueDate();

        int missedDays = 0;
        if (nextDueDate != null && paymentDate.isAfter(nextDueDate)) {
            missedDays = (int) (paymentDate.toEpochDay() - nextDueDate.toEpochDay());
        }

        boolean isMissed = amountPaid.compareTo(BigDecimal.ZERO) == 0;
        boolean isPartial = false;
        boolean isAdvance = false;
        boolean isLate = false;
        boolean isPreClosure = false;
        boolean isEarly = false;

        BigDecimal allocatedToPenalty = BigDecimal.ZERO;
        BigDecimal allocatedToOverdue = BigDecimal.ZERO;
        BigDecimal allocatedToTodayDue = BigDecimal.ZERO;
        BigDecimal allocatedToAdvance = BigDecimal.ZERO;

        BigDecimal penaltyAdded = BigDecimal.ZERO;
        BigDecimal interestAdded = BigDecimal.ZERO;

        if (missedDays > 0) {
            overdueAmount = overdueAmount.add(dailyDue.multiply(BigDecimal.valueOf(missedDays)));
            loan.setMissedDaysCount((loan.getMissedDaysCount() == null ? 0 : loan.getMissedDaysCount()) + missedDays);
            loan.setConsecutiveMissedDays((loan.getConsecutiveMissedDays() == null ? 0 : loan.getConsecutiveMissedDays()) + missedDays);
        }

        if ((loan.getConsecutiveMissedDays() != null ? loan.getConsecutiveMissedDays() : 0) >= 4) {
            penaltyAdded = dailyDue.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal currentPayable = overdueAmount.add(dailyDue).add(penaltyAmount).add(penaltyAdded);

        PaymentStatus paymentStatus;

        if (amountPaid.compareTo(remainingAmount.add(overdueAmount).add(penaltyAmount).add(penaltyAdded)) >= 0) {
            paymentStatus = PaymentStatus.PRE_CLOSURE;
            isPreClosure = true;
            isEarly = true;

            allocatedToPenalty = penaltyAmount.add(penaltyAdded);
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            allocatedToOverdue = overdueAmount.min(afterPenalty);
            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            allocatedToTodayDue = dailyDue.min(afterOverdue);
            BigDecimal afterTodayDue = afterOverdue.subtract(allocatedToTodayDue);

            allocatedToAdvance = afterTodayDue;

            remainingAmount = BigDecimal.ZERO;
            overdueAmount = BigDecimal.ZERO;
            penaltyAmount = BigDecimal.ZERO;

            loan.setIsClosed(true);
            loan.setClosedEarly(true);
            loan.setEndDate(paymentDate);

        } else if (isMissed) {
            paymentStatus = PaymentStatus.MISSED;
            overdueAmount = currentPayable;

            loan.setMissedDaysCount((loan.getMissedDaysCount() == null ? 0 : loan.getMissedDaysCount()) + 1);
            loan.setConsecutiveMissedDays((loan.getConsecutiveMissedDays() == null ? 0 : loan.getConsecutiveMissedDays()) + 1);

            if (missedDays == 0) {
                missedDays = 1;
            }

        } else if (amountPaid.compareTo(currentPayable) < 0) {
            paymentStatus = PaymentStatus.PARTIAL;
            isPartial = true;

            allocatedToPenalty = amountPaid.min(penaltyAmount.add(penaltyAdded));
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            if (afterPenalty.compareTo(BigDecimal.ZERO) > 0) {
                allocatedToOverdue = afterPenalty.min(overdueAmount);
            }

            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            if (afterOverdue.compareTo(BigDecimal.ZERO) > 0) {
                allocatedToTodayDue = afterOverdue.min(dailyDue);
            }

            overdueAmount = currentPayable.subtract(amountPaid);

            loan.setPartialDaysCount((loan.getPartialDaysCount() == null ? 0 : loan.getPartialDaysCount()) + 1);
            loan.setConsecutivePartialDays((loan.getConsecutivePartialDays() == null ? 0 : loan.getConsecutivePartialDays()) + 1);

            if ((loan.getConsecutivePartialDays() != null ? loan.getConsecutivePartialDays() : 0) >= 7) {
                penaltyAdded = penaltyAdded.add(dailyDue.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP));
            }

        } else if (amountPaid.compareTo(currentPayable) == 0) {
            if (missedDays > 0 || overdueAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentStatus = PaymentStatus.LATE_FULL;
                isLate = true;
            } else {
                paymentStatus = PaymentStatus.FULL;
            }

            allocatedToPenalty = penaltyAmount.add(penaltyAdded);
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            allocatedToOverdue = overdueAmount.min(afterPenalty);
            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            allocatedToTodayDue = dailyDue.min(afterOverdue);

            overdueAmount = BigDecimal.ZERO;
            penaltyAmount = BigDecimal.ZERO;
            loan.setConsecutiveMissedDays(0);
            loan.setConsecutivePartialDays(0);

        } else {
            paymentStatus = PaymentStatus.ADVANCE;
            isAdvance = true;
            isEarly = true;

            allocatedToPenalty = penaltyAmount.add(penaltyAdded);
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            allocatedToOverdue = overdueAmount.min(afterPenalty);
            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            allocatedToTodayDue = dailyDue.min(afterOverdue);
            BigDecimal afterTodayDue = afterOverdue.subtract(allocatedToTodayDue);

            allocatedToAdvance = afterTodayDue;

            overdueAmount = BigDecimal.ZERO;
            penaltyAmount = BigDecimal.ZERO;

            int extraDaysCovered = dailyDue.compareTo(BigDecimal.ZERO) == 0
                    ? 0
                    : allocatedToAdvance.divide(dailyDue, 0, RoundingMode.DOWN).intValue();

            loan.setAdvancePaidDaysCount((loan.getAdvancePaidDaysCount() == null ? 0 : loan.getAdvancePaidDaysCount()) + extraDaysCovered);
            loan.setNextDueDate(paymentDate.plusDays(extraDaysCovered + 1));
        }

        BigDecimal actualReduction = amountPaid.subtract(allocatedToPenalty);
        remainingAmount = remainingAmount.subtract(actualReduction);

        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        loan.setOverdueAmount(overdueAmount);
        loan.setPenaltyAmount(penaltyAmount.add(penaltyAdded).doubleValue());
        loan.setRemainingAmount(remainingAmount.add(overdueAmount).add(BigDecimal.valueOf(loan.getPenaltyAmount())).doubleValue());
        loan.setLastPaymentDate(paymentDate);
        loan.setTotalPaidAmount((loan.getTotalPaidAmount() == null ? BigDecimal.ZERO : loan.getTotalPaidAmount()).add(amountPaid));
        loan.setTotalPenaltyAmount(BigDecimal.valueOf(loan.getPenaltyAmount()));

        if (loan.getNextDueDate() == null) {
            loan.setNextDueDate(paymentDate.plusDays(1));
        } else if (!isAdvance) {
            loan.setNextDueDate(paymentDate.plusDays(1));
        }

        if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            loan.setIsClosed(true);

            if (loan.getEndDate() != null && paymentDate.isAfter(loan.getEndDate())) {
                loan.setClosedLate(true);
            }

            if (loan.getTotalRepayableAmount() != null
                    && loan.getDisbursedAmount() != null
                    && !Boolean.TRUE.equals(loan.getPlatformFeeCharged())) {

                BigDecimal lenderProfit = loan.getTotalRepayableAmount().subtract(loan.getDisbursedAmount());

                if (lenderProfit.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal platformFee = lenderProfit
                            .multiply(loan.getPlatformFeeRate())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    loan.setPlatformFeeAmount(platformFee);
                    loan.setPlatformFeeCharged(true);
                }
            }
        }

        loanRepository.save(loan);

        Repayment repayment = Repayment.builder()
                .borrowerId(loan.getBorrowerId())
                .loan(loan)
                .loanApplication(loanApplication)
                .amountPaid(amountPaid.doubleValue())
                .paymentDate(paymentDate)
                .paymentMode(paymentMode)
                .paymentStatus(paymentStatus)
                .transactionReference(UUID.randomUUID().toString())
                .isPartialPayment(isPartial)
                .isEarlyPayment(isEarly)
                .isMissedPayment(isMissed)
                .isAdvancePayment(isAdvance)
                .isLatePayment(isLate)
                .isPreClosure(isPreClosure)
                .interestAdded(interestAdded.doubleValue())
                .penaltyAmount(penaltyAdded.doubleValue())
                .missedDays(missedDays)
                .balanceAmount(loan.getRemainingAmount())
                .allocatedToPenalty(allocatedToPenalty.doubleValue())
                .allocatedToOverdue(allocatedToOverdue.doubleValue())
                .allocatedToTodayDue(allocatedToTodayDue.doubleValue())
                .allocatedToAdvance(allocatedToAdvance.doubleValue())
                .daysCovered(
                        dailyDue.compareTo(BigDecimal.ZERO) == 0
                                ? 0
                                : amountPaid.divide(dailyDue, 0, RoundingMode.DOWN).intValue()
                )
                .build();

        Repayment savedRepayment = repaymentRepository.save(repayment);

        if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentMode == PaymentMode.WALLET) {
                walletService.debitWallet(
                        WalletOwnerType.BORROWER,
                        loan.getBorrowerId(),
                        amountPaid,
                        WalletTransactionType.REPAYMENT_DEBIT,
                        "Borrower repaid loan through wallet",
                        loan.getLoanId(),
                        savedRepayment.getId()
                );
            }

            walletService.creditWallet(
                    WalletOwnerType.LENDER,
                    loan.getLenderId(),
                    amountPaid,
                    WalletTransactionType.REPAYMENT_CREDIT,
                    paymentMode == PaymentMode.WALLET
                            ? "Lender received loan repayment through wallet"
                            : "Lender received cash repayment (ledger recorded)",
                    loan.getLoanId(),
                    savedRepayment.getId()
            );
        }

        if (Boolean.TRUE.equals(loan.getIsClosed())
                && Boolean.TRUE.equals(loan.getPlatformFeeCharged())
                && !Boolean.TRUE.equals(loan.getPlatformFeeTransferred())
                && loan.getPlatformFeeAmount() != null
                && loan.getPlatformFeeAmount().compareTo(BigDecimal.ZERO) > 0) {

            walletService.debitWallet(
                    WalletOwnerType.LENDER,
                    loan.getLenderId(),
                    loan.getPlatformFeeAmount(),
                    WalletTransactionType.PLATFORM_FEE_DEBIT,
                    "Platform fee debited on successful loan closure",
                    loan.getLoanId(),
                    savedRepayment.getId()
            );

            walletService.creditWallet(
                    WalletOwnerType.PLATFORM,
                    0L,
                    loan.getPlatformFeeAmount(),
                    WalletTransactionType.PLATFORM_FEE_CREDIT,
                    "Platform fee credited from lender profit",
                    loan.getLoanId(),
                    savedRepayment.getId()
            );

            loan.setPlatformFeeTransferred(true);
            loanRepository.save(loan);
        }

        return mapToDTO(savedRepayment);
    }

    @Override
    public List<RepaymentResponseDTO> getByLoan(Long loanId) {
        return repaymentRepository.findByLoanLoanId(loanId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RepaymentResponseDTO> getByLoanApplication(Long loanApplicationId) {
        return repaymentRepository.findByLoanApplicationApplicationId(loanApplicationId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RepaymentResponseDTO> getByBorrower(Long borrowerId) {
        return repaymentRepository.findByBorrowerIdOrderByPaymentDateDesc(borrowerId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private RepaymentResponseDTO mapToDTO(Repayment r) {
        return RepaymentResponseDTO.builder()
                .id(r.getId())
                .borrowerId(r.getBorrowerId())
                .amountPaid(r.getAmountPaid())
                .paymentDate(r.getPaymentDate())
                .paymentMode(r.getPaymentMode())
                .paymentStatus(r.getPaymentStatus())
                .isPartialPayment(r.getIsPartialPayment())
                .isEarlyPayment(r.getIsEarlyPayment())
                .isMissedPayment(r.getIsMissedPayment())
                .isAdvancePayment(r.getIsAdvancePayment())
                .isLatePayment(r.getIsLatePayment())
                .isPreClosure(r.getIsPreClosure())
                .interestAdded(r.getInterestAdded())
                .penaltyAmount(r.getPenaltyAmount())
                .missedDays(r.getMissedDays())
                .balanceAmount(r.getBalanceAmount())
                .allocatedToOverdue(r.getAllocatedToOverdue())
                .allocatedToTodayDue(r.getAllocatedToTodayDue())
                .allocatedToAdvance(r.getAllocatedToAdvance())
                .allocatedToPenalty(r.getAllocatedToPenalty())
                .daysCovered(r.getDaysCovered())
                .transactionReference(r.getTransactionReference())
                .build();
    }
}