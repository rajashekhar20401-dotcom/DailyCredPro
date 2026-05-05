package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApplicationStatus;
import com.unqiuehire.kashflow.constant.PaymentMode;
import com.unqiuehire.kashflow.constant.PaymentStatus;
import com.unqiuehire.kashflow.constant.PenaltyReasonType;
import com.unqiuehire.kashflow.constant.RewardReasonType;
import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.constant.WalletTransactionType;
import com.unqiuehire.kashflow.dto.requestdto.RepaymentRequestDTO;
import com.unqiuehire.kashflow.dto.responsedto.RepaymentResponseDTO;
import com.unqiuehire.kashflow.entity.AdminAccount;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.LoanApplication;
import com.unqiuehire.kashflow.entity.PenaltyEvent;
import com.unqiuehire.kashflow.entity.Repayment;
import com.unqiuehire.kashflow.entity.RewardEvent;
import com.unqiuehire.kashflow.repository.AdminAccountRepository;
import com.unqiuehire.kashflow.repository.LoanApplicationRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.repository.PenaltyEventRepository;
import com.unqiuehire.kashflow.repository.RepaymentRepository;
import com.unqiuehire.kashflow.repository.RewardEventRepository;
import com.unqiuehire.kashflow.service.RepaymentService;
import com.unqiuehire.kashflow.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepaymentServiceImpl implements RepaymentService {

    private static final int DEFAULT_PENALTY_TRIGGER_MISSED_DAYS = 5;
    private static final int DEFAULT_PENALTY_TRIGGER_PARTIAL_DAYS = 10;
    private static final BigDecimal DEFAULT_PENALTY_PERCENT_OF_DAILY_INTEREST = BigDecimal.valueOf(1.0);
    private static final BigDecimal DEFAULT_PLATFORM_FEE_PERCENT = BigDecimal.valueOf(1.0);
    private static final BigDecimal DEFAULT_REWARD_PERCENT = BigDecimal.valueOf(1.0);

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final WalletService walletService;
    private final AdminAccountRepository adminAccountRepository;
    private final PenaltyEventRepository penaltyEventRepository;
    private final RewardEventRepository rewardEventRepository;

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

        AdminAccount policyAdmin = resolvePolicyAdmin();

        int penaltyTriggerMissedDays = resolvePenaltyTriggerMissedDays(policyAdmin);
        int penaltyTriggerPartialDays = resolvePenaltyTriggerPartialDays(policyAdmin);
        BigDecimal penaltyPercentOfDailyInterest = resolvePenaltyPercent(policyAdmin);
        BigDecimal platformFeePercent = resolvePlatformFeePercent(policyAdmin);

        LocalDate today = LocalDate.now();

        if (request.getPaymentDate() != null && !request.getPaymentDate().equals(today)) {
            throw new RuntimeException("Repayment date must be today. Future or backdated repayment is not allowed from this screen.");
        }

        LocalDate paymentDate = today;
        PaymentMode paymentMode = request.getPaymentMode() == null ? PaymentMode.CASH : request.getPaymentMode();

        BigDecimal amountPaid = safeBig(request.getAmountPaid());
        BigDecimal dailyDue = safeBig(loan.getDailyEmi());
        BigDecimal overdueAmount = safeBig(loan.getOverdueAmount());
        BigDecimal outstandingPenaltyAtStart = safeBig(loan.getPenaltyAmount());
        BigDecimal cumulativePenaltyAccrued = safeBig(loan.getTotalPenaltyAmount());

        BigDecimal contractualTotalRepayable = safeBig(loan.getTotalRepayableAmount());
        BigDecimal totalPaidBefore = safeBig(loan.getTotalPaidAmount());
        BigDecimal totalInterestRebateBefore = safeBig(loan.getTotalInterestRebateAmount());

        BigDecimal effectiveTotalRepayableBefore = contractualTotalRepayable.subtract(totalInterestRebateBefore);
        if (effectiveTotalRepayableBefore.compareTo(BigDecimal.ZERO) < 0) {
            effectiveTotalRepayableBefore = BigDecimal.ZERO;
        }

        BigDecimal coreRemainingBefore = effectiveTotalRepayableBefore.subtract(totalPaidBefore);
        if (coreRemainingBefore.compareTo(BigDecimal.ZERO) < 0) {
            coreRemainingBefore = BigDecimal.ZERO;
        }

        BigDecimal dailyInterestAmount = deriveDailyInterestAmount(loan);
        BigDecimal penaltyUnitAmount = dailyInterestAmount
                .multiply(penaltyPercentOfDailyInterest)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        LocalDate nextDueDate = loan.getNextDueDate() == null ? loan.getStartDate() : loan.getNextDueDate();

        int previousConsecutiveMissedDays = safeInt(loan.getConsecutiveMissedDays());
        int previousConsecutivePartialDays = safeInt(loan.getConsecutivePartialDays());

        int missedDaysByCalendar = 0;
        if (nextDueDate != null && paymentDate.isAfter(nextDueDate)) {
            missedDaysByCalendar = (int) (paymentDate.toEpochDay() - nextDueDate.toEpochDay());
        }

        int additionalMissedDaysForHistory = 0;
        int newConsecutiveMissedDays = previousConsecutiveMissedDays;
        int newConsecutivePartialDays = previousConsecutivePartialDays;

        if (missedDaysByCalendar > 0) {
            overdueAmount = overdueAmount.add(dailyDue.multiply(BigDecimal.valueOf(missedDaysByCalendar)));
            additionalMissedDaysForHistory += missedDaysByCalendar;
            newConsecutiveMissedDays += missedDaysByCalendar;
        }

        boolean isMissed = amountPaid.compareTo(BigDecimal.ZERO) == 0;
        boolean isPartial = false;
        boolean isAdvance = false;
        boolean isLate = false;
        boolean isPreClosure = false;
        boolean isEarly = false;

        if (isMissed) {
            additionalMissedDaysForHistory += 1;
            newConsecutiveMissedDays += 1;
            newConsecutivePartialDays = 0;
        }

        BigDecimal penaltyAddedFromMissed = calculatePenaltyForMissedStreak(
                penaltyUnitAmount,
                previousConsecutiveMissedDays,
                newConsecutiveMissedDays,
                penaltyTriggerMissedDays
        );

        BigDecimal outstandingPenaltyBeforePayment = outstandingPenaltyAtStart.add(penaltyAddedFromMissed);

        BigDecimal currentCorePayable = overdueAmount.add(dailyDue);
        if (currentCorePayable.compareTo(coreRemainingBefore) > 0) {
            currentCorePayable = coreRemainingBefore;
        }

        BigDecimal currentPayable = currentCorePayable.add(outstandingPenaltyBeforePayment);

        BigDecimal fullAvailableRebate = deriveAvailableInterestRebateAsOfDate(loan, paymentDate);
        BigDecimal preclosureSettlementAmount = coreRemainingBefore
                .add(outstandingPenaltyBeforePayment)
                .subtract(fullAvailableRebate);

        if (preclosureSettlementAmount.compareTo(BigDecimal.ZERO) < 0) {
            preclosureSettlementAmount = BigDecimal.ZERO;
        }

        BigDecimal allocatedToPenalty = BigDecimal.ZERO;
        BigDecimal allocatedToOverdue = BigDecimal.ZERO;
        BigDecimal allocatedToTodayDue = BigDecimal.ZERO;
        BigDecimal allocatedToAdvance = BigDecimal.ZERO;

        BigDecimal outstandingPenaltyAfter = outstandingPenaltyBeforePayment;
        BigDecimal overdueAmountAfter = overdueAmount;
        BigDecimal coreRemainingAfter = coreRemainingBefore;
        BigDecimal interestRebateApplied = BigDecimal.ZERO;
        BigDecimal penaltyAddedFromPartial = BigDecimal.ZERO;

        PaymentStatus paymentStatus;

        if (!isMissed && amountPaid.compareTo(preclosureSettlementAmount) >= 0 && preclosureSettlementAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentStatus = PaymentStatus.PRE_CLOSURE;
            isPreClosure = true;
            isEarly = true;

            allocatedToPenalty = amountPaid.min(outstandingPenaltyBeforePayment);
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            allocatedToOverdue = afterPenalty.min(overdueAmount);
            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            BigDecimal todayDuePortion = dailyDue;
            if (todayDuePortion.compareTo(coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO)) > 0) {
                todayDuePortion = coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO);
            }

            allocatedToTodayDue = afterOverdue.min(todayDuePortion);
            BigDecimal afterTodayDue = afterOverdue.subtract(allocatedToTodayDue);

            allocatedToAdvance = afterTodayDue.max(BigDecimal.ZERO);

            interestRebateApplied = fullAvailableRebate;
            outstandingPenaltyAfter = BigDecimal.ZERO;
            overdueAmountAfter = BigDecimal.ZERO;

            BigDecimal actualReductionFromRepayable = amountPaid.subtract(allocatedToPenalty);
            coreRemainingAfter = coreRemainingBefore
                    .subtract(actualReductionFromRepayable)
                    .subtract(interestRebateApplied);

            if (coreRemainingAfter.compareTo(BigDecimal.ZERO) < 0) {
                coreRemainingAfter = BigDecimal.ZERO;
            }

            newConsecutiveMissedDays = 0;
            newConsecutivePartialDays = 0;

            loan.setIsClosed(true);
            loan.setClosedEarly(true);
            loan.setEndDate(paymentDate);
        } else if (isMissed) {
            paymentStatus = PaymentStatus.MISSED;
            overdueAmountAfter = currentCorePayable;
            outstandingPenaltyAfter = outstandingPenaltyBeforePayment;
        } else if (amountPaid.compareTo(currentPayable) < 0) {
            paymentStatus = PaymentStatus.PARTIAL;
            isPartial = true;

            allocatedToPenalty = amountPaid.min(outstandingPenaltyBeforePayment);
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            allocatedToOverdue = afterPenalty.min(overdueAmount);
            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            BigDecimal todayDuePortion = dailyDue;
            if (todayDuePortion.compareTo(coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO)) > 0) {
                todayDuePortion = coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO);
            }

            allocatedToTodayDue = afterOverdue.min(todayDuePortion);

            BigDecimal paidTowardCore = amountPaid.subtract(allocatedToPenalty);
            BigDecimal unpaidCurrentCore = currentCorePayable.subtract(paidTowardCore);
            if (unpaidCurrentCore.compareTo(BigDecimal.ZERO) < 0) {
                unpaidCurrentCore = BigDecimal.ZERO;
            }

            overdueAmountAfter = unpaidCurrentCore;
            outstandingPenaltyAfter = outstandingPenaltyBeforePayment.subtract(allocatedToPenalty);

            newConsecutiveMissedDays = 0;
            newConsecutivePartialDays = previousConsecutivePartialDays + 1;

            penaltyAddedFromPartial = calculatePenaltyForPartialStreak(
                    penaltyUnitAmount,
                    previousConsecutivePartialDays,
                    newConsecutivePartialDays,
                    penaltyTriggerPartialDays
            );

            outstandingPenaltyAfter = outstandingPenaltyAfter.add(penaltyAddedFromPartial);

            BigDecimal actualReductionFromRepayable = amountPaid.subtract(allocatedToPenalty);
            coreRemainingAfter = coreRemainingBefore.subtract(actualReductionFromRepayable);
            if (coreRemainingAfter.compareTo(BigDecimal.ZERO) < 0) {
                coreRemainingAfter = BigDecimal.ZERO;
            }
        } else if (amountPaid.compareTo(currentPayable) == 0) {
            if (missedDaysByCalendar > 0 || overdueAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentStatus = PaymentStatus.LATE_FULL;
                isLate = true;
            } else {
                paymentStatus = PaymentStatus.FULL;
            }

            allocatedToPenalty = outstandingPenaltyBeforePayment;
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            allocatedToOverdue = afterPenalty.min(overdueAmount);
            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            BigDecimal todayDuePortion = dailyDue;
            if (todayDuePortion.compareTo(coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO)) > 0) {
                todayDuePortion = coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO);
            }

            allocatedToTodayDue = afterOverdue.min(todayDuePortion);

            outstandingPenaltyAfter = BigDecimal.ZERO;
            overdueAmountAfter = BigDecimal.ZERO;

            newConsecutiveMissedDays = 0;
            newConsecutivePartialDays = 0;

            BigDecimal actualReductionFromRepayable = amountPaid.subtract(allocatedToPenalty);
            coreRemainingAfter = coreRemainingBefore.subtract(actualReductionFromRepayable);
            if (coreRemainingAfter.compareTo(BigDecimal.ZERO) < 0) {
                coreRemainingAfter = BigDecimal.ZERO;
            }
        } else {
            paymentStatus = PaymentStatus.ADVANCE;
            isAdvance = true;
            isEarly = true;

            allocatedToPenalty = outstandingPenaltyBeforePayment;
            BigDecimal afterPenalty = amountPaid.subtract(allocatedToPenalty);

            allocatedToOverdue = afterPenalty.min(overdueAmount);
            BigDecimal afterOverdue = afterPenalty.subtract(allocatedToOverdue);

            BigDecimal todayDuePortion = dailyDue;
            if (todayDuePortion.compareTo(coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO)) > 0) {
                todayDuePortion = coreRemainingBefore.subtract(overdueAmount).max(BigDecimal.ZERO);
            }

            allocatedToTodayDue = afterOverdue.min(todayDuePortion);
            BigDecimal afterTodayDue = afterOverdue.subtract(allocatedToTodayDue);

            allocatedToAdvance = afterTodayDue.max(BigDecimal.ZERO);

            outstandingPenaltyAfter = BigDecimal.ZERO;
            overdueAmountAfter = BigDecimal.ZERO;

            int extraDaysCovered = dailyDue.compareTo(BigDecimal.ZERO) == 0
                    ? 0
                    : allocatedToAdvance.divide(dailyDue, 0, RoundingMode.DOWN).intValue();

            interestRebateApplied = deriveAdvanceInterestRebate(loan, paymentDate, extraDaysCovered);

            newConsecutiveMissedDays = 0;
            newConsecutivePartialDays = 0;

            BigDecimal actualReductionFromRepayable = amountPaid.subtract(allocatedToPenalty);
            coreRemainingAfter = coreRemainingBefore
                    .subtract(actualReductionFromRepayable)
                    .subtract(interestRebateApplied);

            if (coreRemainingAfter.compareTo(BigDecimal.ZERO) < 0) {
                coreRemainingAfter = BigDecimal.ZERO;
            }

            loan.setAdvancePaidDaysCount(safeInt(loan.getAdvancePaidDaysCount()) + extraDaysCovered);
            loan.setNextDueDate(paymentDate.plusDays(extraDaysCovered + 1));
        }

        BigDecimal penaltyAddedToday = penaltyAddedFromMissed.add(penaltyAddedFromPartial);
        cumulativePenaltyAccrued = cumulativePenaltyAccrued.add(penaltyAddedToday);

        loan.setMissedDaysCount(safeInt(loan.getMissedDaysCount()) + additionalMissedDaysForHistory);
        loan.setConsecutiveMissedDays(newConsecutiveMissedDays);
        loan.setPartialDaysCount(safeInt(loan.getPartialDaysCount()) + (isPartial ? 1 : 0));
        loan.setConsecutivePartialDays(newConsecutivePartialDays);

        loan.setOverdueAmount(overdueAmountAfter);
        loan.setPenaltyAmount(outstandingPenaltyAfter.doubleValue());
        loan.setRemainingAmount(coreRemainingAfter.doubleValue());
        loan.setPrincipalOutstanding(coreRemainingAfter);
        loan.setLastPaymentDate(paymentDate);

        loan.setTotalPaidAmount(totalPaidBefore.add(amountPaid));
        loan.setTotalPenaltyAmount(cumulativePenaltyAccrued);
        loan.setTotalInterestRebateAmount(totalInterestRebateBefore.add(interestRebateApplied));

        if (loan.getNextDueDate() == null) {
            loan.setNextDueDate(paymentDate.plusDays(1));
        } else if (!isAdvance && !Boolean.TRUE.equals(loan.getIsClosed())) {
            loan.setNextDueDate(paymentDate.plusDays(1));
        }

        if (coreRemainingAfter.compareTo(BigDecimal.ZERO) == 0
                && overdueAmountAfter.compareTo(BigDecimal.ZERO) == 0
                && outstandingPenaltyAfter.compareTo(BigDecimal.ZERO) == 0) {
            loan.setIsClosed(true);

            if (loan.getEndDate() != null && paymentDate.isAfter(loan.getEndDate())) {
                loan.setClosedLate(true);
            }

            if (loan.getTotalRepayableAmount() != null
                    && loan.getDisbursedAmount() != null
                    && !Boolean.TRUE.equals(loan.getPlatformFeeCharged())) {

                BigDecimal lenderProfit = loan.getTotalRepayableAmount()
                        .subtract(safeBig(loan.getTotalInterestRebateAmount()))
                        .subtract(loan.getDisbursedAmount());

                if (lenderProfit.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal platformFee = lenderProfit
                            .multiply(platformFeePercent)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    loan.setPlatformFeeRate(platformFeePercent);
                    loan.setPlatformFeeAmount(platformFee);
                    loan.setPlatformFeeCharged(true);
                }
            }
        }

        loanRepository.save(loan);

        BigDecimal outstandingBalanceAfterPayment = coreRemainingAfter
                .add(overdueAmountAfter)
                .add(outstandingPenaltyAfter);

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
                .interestAdded(0.0)
                .interestRebateApplied(interestRebateApplied.doubleValue())
                .penaltyAmount(penaltyAddedToday.doubleValue())
                .missedDays(additionalMissedDaysForHistory)
                .balanceAmount(outstandingBalanceAfterPayment.doubleValue())
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

        if (penaltyAddedFromMissed.compareTo(BigDecimal.ZERO) > 0) {
            savePenaltyEvent(
                    loan,
                    savedRepayment.getId(),
                    paymentDate,
                    PenaltyReasonType.MISSED_STREAK_PENALTY,
                    penaltyAddedFromMissed,
                    newConsecutiveMissedDays,
                    penaltyTriggerMissedDays,
                    dailyInterestAmount,
                    penaltyPercentOfDailyInterest,
                    "Penalty applied because consecutive missed days crossed the allowed threshold."
            );
        }

        if (penaltyAddedFromPartial.compareTo(BigDecimal.ZERO) > 0) {
            savePenaltyEvent(
                    loan,
                    savedRepayment.getId(),
                    paymentDate,
                    PenaltyReasonType.PARTIAL_STREAK_PENALTY,
                    penaltyAddedFromPartial,
                    newConsecutivePartialDays,
                    penaltyTriggerPartialDays,
                    dailyInterestAmount,
                    penaltyPercentOfDailyInterest,
                    "Penalty applied because consecutive partial payments crossed the allowed threshold."
            );
        }

        BigDecimal rewardAmount = calculateRewardAmount(paymentStatus, amountPaid, interestRebateApplied);

        if (rewardAmount.compareTo(BigDecimal.ZERO) > 0) {
            saveRewardEvent(
                    loan,
                    savedRepayment.getId(),
                    paymentDate,
                    paymentStatus == PaymentStatus.PRE_CLOSURE
                            ? RewardReasonType.PRE_CLOSURE_REWARD
                            : RewardReasonType.ADVANCE_REPAYMENT_REWARD,
                    amountPaid,
                    rewardAmount,
                    DEFAULT_REWARD_PERCENT,
                    "Reward tracked for early repayment behavior. This is stored for reward reporting and can later be credited to wallet or loyalty points."
            );
        }

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
                .interestRebateApplied(r.getInterestRebateApplied())
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

    private BigDecimal deriveAdvanceInterestRebate(Loan loan, LocalDate paymentDate, int extraDaysCovered) {
        if (extraDaysCovered <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal dailyInterestAmount = deriveDailyInterestAmount(loan);
        BigDecimal requestedRebate = dailyInterestAmount.multiply(BigDecimal.valueOf(extraDaysCovered));
        BigDecimal availableRebate = deriveAvailableInterestRebateAsOfDate(loan, paymentDate);

        return requestedRebate.min(availableRebate).max(BigDecimal.ZERO);
    }

    private BigDecimal deriveAvailableInterestRebateAsOfDate(Loan loan, LocalDate paymentDate) {
        BigDecimal totalInterest = safeBig(loan.getInterestDeductionAmount());
        BigDecimal alreadyRebated = safeBig(loan.getTotalInterestRebateAmount());
        BigDecimal dailyInterestAmount = deriveDailyInterestAmount(loan);

        int tenureDays = safeInt(loan.getTenureDays());
        if (tenureDays <= 0 || totalInterest.compareTo(BigDecimal.ZERO) <= 0 || dailyInterestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate startDate = loan.getStartDate();
        if (startDate == null) {
            return BigDecimal.ZERO;
        }

        int elapsedDays;
        if (paymentDate.isBefore(startDate)) {
            elapsedDays = 0;
        } else {
            elapsedDays = (int) (paymentDate.toEpochDay() - startDate.toEpochDay()) + 1;
        }

        if (elapsedDays < 0) {
            elapsedDays = 0;
        }

        if (elapsedDays > tenureDays) {
            elapsedDays = tenureDays;
        }

        int remainingUnearnedDays = tenureDays - elapsedDays;
        if (remainingUnearnedDays <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxRebateAsOfDate = dailyInterestAmount
                .multiply(BigDecimal.valueOf(remainingUnearnedDays))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal available = maxRebateAsOfDate.subtract(alreadyRebated);
        return available.max(BigDecimal.ZERO);
    }

    private BigDecimal calculatePenaltyForMissedStreak(
            BigDecimal penaltyUnitAmount,
            int previousConsecutiveMissedDays,
            int currentConsecutiveMissedDays,
            int triggerDays
    ) {
        int previouslyPenalizedDays = Math.max(0, previousConsecutiveMissedDays - triggerDays);
        int currentlyPenalizedDays = Math.max(0, currentConsecutiveMissedDays - triggerDays);
        int newlyPenalizedDays = Math.max(0, currentlyPenalizedDays - previouslyPenalizedDays);

        return penaltyUnitAmount.multiply(BigDecimal.valueOf(newlyPenalizedDays));
    }

    private BigDecimal calculatePenaltyForPartialStreak(
            BigDecimal penaltyUnitAmount,
            int previousConsecutivePartialDays,
            int currentConsecutivePartialDays,
            int triggerDays
    ) {
        int previouslyPenalizedDays = Math.max(0, previousConsecutivePartialDays - triggerDays);
        int currentlyPenalizedDays = Math.max(0, currentConsecutivePartialDays - triggerDays);
        int newlyPenalizedDays = Math.max(0, currentlyPenalizedDays - previouslyPenalizedDays);

        return penaltyUnitAmount.multiply(BigDecimal.valueOf(newlyPenalizedDays));
    }

    private BigDecimal calculateRewardAmount(PaymentStatus paymentStatus, BigDecimal amountPaid, BigDecimal interestRebateApplied) {
        if (paymentStatus != PaymentStatus.ADVANCE && paymentStatus != PaymentStatus.PRE_CLOSURE) {
            return BigDecimal.ZERO;
        }

        if (interestRebateApplied == null || interestRebateApplied.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rawReward = amountPaid
                .multiply(DEFAULT_REWARD_PERCENT)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return rawReward.min(interestRebateApplied).max(BigDecimal.ZERO);
    }

    private BigDecimal deriveDailyInterestAmount(Loan loan) {
        if (loan.getDailyInterestAmount() != null && loan.getDailyInterestAmount().compareTo(BigDecimal.ZERO) > 0) {
            return loan.getDailyInterestAmount();
        }

        int tenureDays = safeInt(loan.getTenureDays());
        if (tenureDays <= 0) {
            return BigDecimal.ZERO;
        }

        if (loan.getInterestDeductionAmount() != null) {
            return loan.getInterestDeductionAmount()
                    .divide(BigDecimal.valueOf(tenureDays), 2, RoundingMode.HALF_UP);
        }

        if (loan.getSanctionedAmount() != null && loan.getDisbursedAmount() != null) {
            BigDecimal totalInterest = BigDecimal.valueOf(loan.getSanctionedAmount())
                    .subtract(loan.getDisbursedAmount());

            if (totalInterest.compareTo(BigDecimal.ZERO) > 0) {
                return totalInterest.divide(BigDecimal.valueOf(tenureDays), 2, RoundingMode.HALF_UP);
            }
        }

        return BigDecimal.ZERO;
    }

    private void savePenaltyEvent(
            Loan loan,
            Long repaymentId,
            LocalDate eventDate,
            PenaltyReasonType reasonType,
            BigDecimal amount,
            int triggerCount,
            int thresholdValue,
            BigDecimal dailyInterestAmount,
            BigDecimal penaltyPercent,
            String note
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        PenaltyEvent event = new PenaltyEvent();
        event.setLoanId(loan.getLoanId());
        event.setRepaymentId(repaymentId);
        event.setBorrowerId(loan.getBorrowerId());
        event.setLenderId(loan.getLenderId());
        event.setEventDate(eventDate);
        event.setReasonType(reasonType);
        event.setTriggerCount(triggerCount);
        event.setThresholdValue(thresholdValue);
        event.setDailyInterestAmount(dailyInterestAmount == null ? BigDecimal.ZERO : dailyInterestAmount.setScale(2, RoundingMode.HALF_UP));
        event.setPenaltyPercent(penaltyPercent == null ? 0.0 : penaltyPercent.doubleValue());
        event.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        event.setNote(note);
        event.setResolved(false);

        penaltyEventRepository.save(event);
    }

    private void saveRewardEvent(
            Loan loan,
            Long repaymentId,
            LocalDate eventDate,
            RewardReasonType reasonType,
            BigDecimal baseAmount,
            BigDecimal rewardAmount,
            BigDecimal rewardPercent,
            String note
    ) {
        if (rewardAmount == null || rewardAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        RewardEvent event = new RewardEvent();
        event.setLoanId(loan.getLoanId());
        event.setRepaymentId(repaymentId);
        event.setBorrowerId(loan.getBorrowerId());
        event.setLenderId(loan.getLenderId());
        event.setEventDate(eventDate);
        event.setReasonType(reasonType);
        event.setRewardPercent(rewardPercent == null ? 0.0 : rewardPercent.doubleValue());
        event.setBaseAmount(baseAmount == null ? BigDecimal.ZERO : baseAmount.setScale(2, RoundingMode.HALF_UP));
        event.setRewardAmount(rewardAmount.setScale(2, RoundingMode.HALF_UP));
        event.setNote(note);

        rewardEventRepository.save(event);
    }

    private AdminAccount resolvePolicyAdmin() {
        List<AdminAccount> admins = adminAccountRepository.findAll();

        if (admins.isEmpty()) {
            return null;
        }

        return admins.stream()
                .filter(admin -> Boolean.TRUE.equals(admin.getActive()))
                .sorted(
                        Comparator.comparing(AdminAccount::getSuperAdmin, Comparator.reverseOrder())
                                .thenComparing(AdminAccount::getAdminId)
                )
                .findFirst()
                .orElse(admins.get(0));
    }

    private int resolvePenaltyTriggerMissedDays(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getPenaltyTriggerMissedDays() != null) {
            return policyAdmin.getPenaltyTriggerMissedDays();
        }
        return DEFAULT_PENALTY_TRIGGER_MISSED_DAYS;
    }

    private int resolvePenaltyTriggerPartialDays(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getPenaltyTriggerPartialDays() != null) {
            return policyAdmin.getPenaltyTriggerPartialDays();
        }
        return DEFAULT_PENALTY_TRIGGER_PARTIAL_DAYS;
    }

    private BigDecimal resolvePenaltyPercent(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getPenaltyPercentOfDailyInterest() != null) {
            return BigDecimal.valueOf(policyAdmin.getPenaltyPercentOfDailyInterest());
        }
        return DEFAULT_PENALTY_PERCENT_OF_DAILY_INTEREST;
    }

    private BigDecimal resolvePlatformFeePercent(AdminAccount policyAdmin) {
        if (policyAdmin != null && policyAdmin.getPlatformFeePercent() != null) {
            return BigDecimal.valueOf(policyAdmin.getPlatformFeePercent());
        }
        return DEFAULT_PLATFORM_FEE_PERCENT;
    }

    private BigDecimal safeBig(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private BigDecimal safeBig(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}