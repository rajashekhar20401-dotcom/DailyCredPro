package com.unqiuehire.kashflow.scheduler;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.entity.AdminAccount;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.repository.AdminAccountRepository;
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
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanMonitoringScheduler {

    private static final int DEFAULT_DEFAULTER_CONSECUTIVE_MISSED_DAYS = 10;

    private final LoanRepository loanRepository;
    private final BorrowerRepository borrowerRepository;
    private final AdminAccountRepository adminAccountRepository;
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
        int defaulterThreshold = resolveDefaulterThreshold();

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

            // Scheduler is only allowed to track overdue and missed-day behaviour.
            // It must not calculate penalty and must not add overdue into remainingAmount.
            loan.setOverdueAmount(currentOverdue.add(newOverdue));
            loan.setMissedDaysCount((loan.getMissedDaysCount() == null ? 0 : loan.getMissedDaysCount()) + overdueDays);
            loan.setConsecutiveMissedDays((loan.getConsecutiveMissedDays() == null ? 0 : loan.getConsecutiveMissedDays()) + overdueDays);

            // Move next due date forward to today so the same overdue period is not counted again tomorrow.
            loan.setNextDueDate(today);

            loanRepository.save(loan);

            if ((loan.getConsecutiveMissedDays() == null ? 0 : loan.getConsecutiveMissedDays()) >= defaulterThreshold) {
                Borrower borrower = borrowerRepository.findById(loan.getBorrowerId()).orElse(null);

                if (borrower != null) {
                    boolean borrowerChanged = false;

                    if (!Boolean.TRUE.equals(borrower.getFrozen())) {
                        borrower.setFrozen(true);
                        borrowerChanged = true;
                    }

                    if (!Boolean.TRUE.equals(borrower.getManualReviewFlag())) {
                        borrower.setManualReviewFlag(true);
                        borrowerChanged = true;
                    }

                    String autoReason = "Auto-frozen by scheduler after " + defaulterThreshold + "+ consecutive missed payment days";
                    if (borrower.getFreezeReason() == null || borrower.getFreezeReason().trim().isEmpty()) {
                        borrower.setFreezeReason(autoReason);
                        borrowerChanged = true;
                    }

                    if (borrowerChanged) {
                        borrowerRepository.save(borrower);
                    }

                    try {
                        walletService.freezeWallet(WalletOwnerType.BORROWER, borrower.getBorrowerId());
                    } catch (Exception e) {
                        log.warn("Failed to freeze borrower wallet for borrowerId={}", borrower.getBorrowerId(), e);
                    }
                }
            }
        }

        log.info("Loan monitoring scheduler completed for date {}", today);
    }

    private int resolveDefaulterThreshold() {
        AdminAccount policyAdmin = resolvePolicyAdmin();

        if (policyAdmin != null && policyAdmin.getDefaulterConsecutiveMissedDays() != null) {
            return policyAdmin.getDefaulterConsecutiveMissedDays();
        }

        return DEFAULT_DEFAULTER_CONSECUTIVE_MISSED_DAYS;
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
}