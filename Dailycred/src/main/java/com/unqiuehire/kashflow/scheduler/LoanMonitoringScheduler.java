package com.unqiuehire.kashflow.scheduler;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanMonitoringScheduler {

    private final LoanRepository loanRepository;
    private final BorrowerRepository borrowerRepository;
    private final WalletService walletService;

    @Value("${scheduler.loan-monitor.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(cron = "${scheduler.loan-monitor.cron:0 5 0 * * *}")
    @Transactional
    public void processDailyOverdues() {
        if (!schedulerEnabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<Loan> loans = loanRepository.findAll();

        for (Loan loan : loans) {
            if (Boolean.TRUE.equals(loan.getIsClosed())) {
                continue;
            }

            if (loan.getNextDueDate() == null || !loan.getNextDueDate().isBefore(today)) {
                continue;
            }

            int overdueDays = (int) (today.toEpochDay() - loan.getNextDueDate().toEpochDay());
            if (overdueDays <= 0) {
                continue;
            }

            BigDecimal dailyDue = BigDecimal.valueOf(loan.getDailyEmi() == null ? 0.0 : loan.getDailyEmi());
            BigDecimal currentOverdue = loan.getOverdueAmount() == null ? BigDecimal.ZERO : loan.getOverdueAmount();
            BigDecimal newOverdue = dailyDue.multiply(BigDecimal.valueOf(overdueDays));

            loan.setOverdueAmount(currentOverdue.add(newOverdue));
            loan.setMissedDaysCount((loan.getMissedDaysCount() == null ? 0 : loan.getMissedDaysCount()) + overdueDays);
            loan.setConsecutiveMissedDays((loan.getConsecutiveMissedDays() == null ? 0 : loan.getConsecutiveMissedDays()) + overdueDays);

            double currentRemaining = loan.getRemainingAmount() == null ? 0.0 : loan.getRemainingAmount();
            loan.setRemainingAmount(currentRemaining + newOverdue.doubleValue());

            if (loan.getConsecutiveMissedDays() >= 4) {
                double currentPenalty = loan.getPenaltyAmount() == null ? 0.0 : loan.getPenaltyAmount();
                double penaltyToAdd = dailyDue.multiply(BigDecimal.valueOf(0.10)).doubleValue();
                loan.setPenaltyAmount(currentPenalty + penaltyToAdd);
                loan.setRemainingAmount(loan.getRemainingAmount() + penaltyToAdd);
            }

            // move next due date forward to today to avoid double counting on next scheduler run
            loan.setNextDueDate(today);

            // if borrower default threshold reached
            if (loan.getConsecutiveMissedDays() >= 10) {
                Borrower borrower = borrowerRepository.findById(loan.getBorrowerId()).orElse(null);
                if (borrower != null) {
                    borrower.setFrozen(true);
                    borrower.setFreezeReason("Auto-frozen by scheduler after 10+ consecutive missed payment days");
                    borrower.setManualReviewFlag(true);
                    borrowerRepository.save(borrower);

                    try {
                        walletService.freezeWallet(WalletOwnerType.BORROWER, borrower.getBorrowerId());
                    } catch (Exception e) {
                        log.warn("Failed to freeze borrower wallet for borrowerId={}", borrower.getBorrowerId(), e);
                    }
                }
            }

            loanRepository.save(loan);
        }

        log.info("Loan monitoring scheduler completed for date {}", today);
    }
}