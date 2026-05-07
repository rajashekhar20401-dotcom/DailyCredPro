package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.ApplicationStatus;
import com.unqiuehire.kashflow.constant.NotificationChannelType;
import com.unqiuehire.kashflow.constant.NotificationTargetType;
import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.constant.WalletTransactionType;
import com.unqiuehire.kashflow.dto.requestdto.LoanRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanResponseDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.LoanApplication;
import com.unqiuehire.kashflow.entity.LoanPlan;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LoanApplicationRepository;
import com.unqiuehire.kashflow.repository.LoanPlanRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.service.LoanCalculationService;
import com.unqiuehire.kashflow.service.LoanService;
import com.unqiuehire.kashflow.service.NotificationService;
import com.unqiuehire.kashflow.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanCalculationService loanCalculationService;
    private final WalletService walletService;
    private final LoanPlanRepository loanPlanRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ApiResponse<LoanResponseDto> createLoan(LoanRequestDto dto) {

        if (dto.getLoanApplicationId() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan application id is required", null);
        }

        if (loanRepository.existsByLoanApplicationId(dto.getLoanApplicationId())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan already exists for this loan application", null);
        }

        Optional<LoanApplication> optionalApplication = loanApplicationRepository.findById(dto.getLoanApplicationId());
        if (optionalApplication.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan application not found", null);
        }

        LoanApplication application = optionalApplication.get();

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan can only be created for approved applications", null);
        }

        if (Boolean.TRUE.equals(application.getIsLoanCreated())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan already created for this application", null);
        }

        Long borrowerId = application.getBorrower().getBorrowerId();
        Long lenderId = application.getLender().getLenderId();
        Long planId = application.getLoanPlan().getId();

        BigDecimal sanctionedAmount = BigDecimal.valueOf(
                dto.getSanctionedAmount() != null ? dto.getSanctionedAmount() : application.getLoanAmount()
        );

        LoanPlan plan = loanPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Loan Plan not found"));

        BigDecimal interestPercent;
        int tenureDays;
        boolean isCustomLoan = Boolean.TRUE.equals(dto.getIsCustomLoan());

        if (isCustomLoan) {
            if (dto.getCustomInterestPercent() == null || dto.getCustomTenureDays() == null || dto.getCustomTenureDays() <= 0) {
                return new ApiResponse<>(ApiStatus.FAILURE, "Custom interest percent and custom tenure days are required for custom loan", null);
            }
            interestPercent = BigDecimal.valueOf(dto.getCustomInterestPercent());
            tenureDays = dto.getCustomTenureDays();
        } else {
            interestPercent = BigDecimal.valueOf(plan.getInterestPerDay());
            tenureDays = plan.getPlanDuration();
        }

        BigDecimal upfrontInterest = loanCalculationService.calculateUpfrontInterest(sanctionedAmount, interestPercent);
        BigDecimal disbursedAmount = loanCalculationService.calculateDisbursedAmount(sanctionedAmount, interestPercent);
        BigDecimal totalRepayableAmount = sanctionedAmount;
        BigDecimal dailyDue = loanCalculationService.calculateDailyDue(totalRepayableAmount, tenureDays);

        BigDecimal dailyInterestAmount = tenureDays <= 0
                ? BigDecimal.ZERO
                : upfrontInterest.divide(BigDecimal.valueOf(tenureDays), 2, java.math.RoundingMode.HALF_UP);
        Loan loan = new Loan();
        loan.setLoanApplicationId(application.getApplicationId());
        loan.setBorrowerId(borrowerId);
        loan.setLenderId(lenderId);
        loan.setPlanId(planId);

        loan.setTotalAmount(sanctionedAmount.doubleValue());
        loan.setSanctionedAmount(sanctionedAmount.doubleValue());
        loan.setInterestPerDay(interestPercent.doubleValue());
        loan.setPenaltyAmount(plan.getPenaltyAmount());
        loan.setTenureDays(tenureDays);
        loan.setStartDate(dto.getStartDate() == null ? LocalDate.now() : dto.getStartDate());
        loan.setEndDate(loan.getStartDate().plusDays(tenureDays));
        loan.setDailyEmi(dailyDue.doubleValue());
        loan.setRemainingAmount(totalRepayableAmount.doubleValue());
        loan.setIsClosed(false);

        loan.setDisbursedAmount(disbursedAmount);
        loan.setInterestDeductionAmount(upfrontInterest);
        loan.setInterestDeductionPercent(interestPercent);
        loan.setDailyInterestAmount(dailyInterestAmount);
        loan.setTotalInterestRebateAmount(BigDecimal.ZERO);

        loan.setTotalRepayableAmount(totalRepayableAmount);
        loan.setPrincipalOutstanding(totalRepayableAmount);
        loan.setOverdueAmount(BigDecimal.ZERO);
        loan.setTotalPenaltyAmount(BigDecimal.ZERO);
        loan.setTotalPaidAmount(BigDecimal.ZERO);
        loan.setMissedDaysCount(0);
        loan.setPartialDaysCount(0);
        loan.setAdvancePaidDaysCount(0);
        loan.setConsecutiveMissedDays(0);
        loan.setConsecutivePartialDays(0);
        loan.setNextDueDate(loan.getStartDate());
        loan.setClosedEarly(false);
        loan.setClosedLate(false);
        loan.setIsCustomLoan(isCustomLoan);
        loan.setCustomInterestPercent(isCustomLoan ? BigDecimal.valueOf(dto.getCustomInterestPercent()) : null);

        loan.setPlatformFeeRate(BigDecimal.valueOf(1.00));
        loan.setPlatformFeeAmount(BigDecimal.ZERO);
        loan.setPlatformFeeCharged(false);

        Loan savedLoan = loanRepository.save(loan);

        walletService.debitWallet(
                WalletOwnerType.LENDER,
                savedLoan.getLenderId(),
                disbursedAmount,
                WalletTransactionType.LOAN_DISBURSEMENT_DEBIT,
                "Loan disbursed to borrower",
                savedLoan.getLoanId(),
                null
        );

        walletService.creditWallet(
                WalletOwnerType.BORROWER,
                savedLoan.getBorrowerId(),
                disbursedAmount,
                WalletTransactionType.LOAN_DISBURSEMENT_CREDIT,
                "Loan amount received from lender",
                savedLoan.getLoanId(),
                null
        );

        application.setIsLoanCreated(true);
        loanApplicationRepository.save(application);

        Borrower borrower = borrowerRepository.findById(savedLoan.getBorrowerId()).orElse(null);
        if (borrower != null) {
            borrower.setTotalLoansTaken((borrower.getTotalLoansTaken() == null ? 0 : borrower.getTotalLoansTaken()) + 1);
            borrowerRepository.save(borrower);
        }

        notificationService.createNotification(
                NotificationTargetType.BORROWER,
                savedLoan.getBorrowerId(),
                NotificationChannelType.IN_APP,
                "Loan Disbursed",
                "Loan amount of " + disbursedAmount + " was disbursed to your borrower wallet for loan " + savedLoan.getLoanId() + "."
        );

        notificationService.createNotification(
                NotificationTargetType.LENDER,
                savedLoan.getLenderId(),
                NotificationChannelType.IN_APP,
                "Loan Created & Disbursed",
                "Loan " + savedLoan.getLoanId() + " was created and disbursed successfully."
        );

        return new ApiResponse<>(ApiStatus.SUCCESS, "Loan created successfully", mapToResponseDto(savedLoan));
    }

    @Override
    public ApiResponse<LoanResponseDto> getLoanById(Long loanId) {
        Optional<Loan> optionalLoan = loanRepository.findById(loanId);

        if (optionalLoan.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan not found", null);
        }

        return new ApiResponse<>(ApiStatus.SUCCESS, "Loan fetched successfully", mapToResponseDto(optionalLoan.get()));
    }

    @Override
    public ApiResponse<List<LoanResponseDto>> getLoansByBorrower(Long borrowerId) {
        List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);
        List<LoanResponseDto> responseList = new ArrayList<>();

        for (Loan loan : loans) {
            responseList.add(mapToResponseDto(loan));
        }

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower loans fetched successfully", responseList);
    }

    @Override
    public ApiResponse<List<LoanResponseDto>> getLoansByLender(Long lenderId) {
        List<Loan> loans = loanRepository.findByLenderId(lenderId);
        List<LoanResponseDto> responseList = new ArrayList<>();

        for (Loan loan : loans) {
            responseList.add(mapToResponseDto(loan));
        }

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender loans fetched successfully", responseList);
    }

    @Override
    public ApiResponse<String> closeLoan(Long loanId) {
        Optional<Loan> optionalLoan = loanRepository.findById(loanId);

        if (optionalLoan.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan not found", null);
        }

        Loan loan = optionalLoan.get();

        if (Boolean.TRUE.equals(loan.getIsClosed())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan is already closed", null);
        }

        loan.setIsClosed(true);
        loan.setRemainingAmount(0.0);
        loan.setPrincipalOutstanding(BigDecimal.ZERO);
        loan.setEndDate(LocalDate.now());

        loanRepository.save(loan);

        notificationService.createNotification(
                NotificationTargetType.BORROWER,
                loan.getBorrowerId(),
                NotificationChannelType.IN_APP,
                "Loan Closed",
                "Loan " + loan.getLoanId() + " has been closed successfully."
        );

        notificationService.createNotification(
                NotificationTargetType.LENDER,
                loan.getLenderId(),
                NotificationChannelType.IN_APP,
                "Loan Closed",
                "Loan " + loan.getLoanId() + " has been closed successfully."
        );

        return new ApiResponse<>(ApiStatus.SUCCESS, "Loan closed successfully", "Loan closed successfully");
    }

    private LoanResponseDto mapToResponseDto(Loan loan) {
        LoanResponseDto dto = new LoanResponseDto();
        dto.setLoanId(loan.getLoanId());
        dto.setLoanApplicationId(loan.getLoanApplicationId());
        dto.setBorrowerId(loan.getBorrowerId());
        dto.setLenderId(loan.getLenderId());
        dto.setPlanId(loan.getPlanId());
        dto.setTotalAmount(loan.getTotalAmount());
        dto.setSanctionedAmount(loan.getSanctionedAmount());
        dto.setInterestPerDay(loan.getInterestPerDay());
        dto.setPenaltyAmount(loan.getPenaltyAmount());
        dto.setRemainingAmount(loan.getRemainingAmount());
        dto.setTenureDays(loan.getTenureDays());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());
        dto.setDailyEmi(loan.getDailyEmi());
        dto.setIsClosed(loan.getIsClosed());

        dto.setDisbursedAmount(loan.getDisbursedAmount() == null ? null : loan.getDisbursedAmount().doubleValue());
        dto.setTotalRepayableAmount(loan.getTotalRepayableAmount() == null ? null : loan.getTotalRepayableAmount().doubleValue());
        dto.setOverdueAmount(loan.getOverdueAmount() == null ? null : loan.getOverdueAmount().doubleValue());
        dto.setTotalPaidAmount(loan.getTotalPaidAmount() == null ? null : loan.getTotalPaidAmount().doubleValue());
        dto.setMissedDaysCount(loan.getMissedDaysCount());
        dto.setPartialDaysCount(loan.getPartialDaysCount());
        dto.setAdvancePaidDaysCount(loan.getAdvancePaidDaysCount());
        dto.setNextDueDate(loan.getNextDueDate());

        dto.setPlatformFeeRate(loan.getPlatformFeeRate() == null ? null : loan.getPlatformFeeRate().doubleValue());
        dto.setPlatformFeeAmount(loan.getPlatformFeeAmount() == null ? null : loan.getPlatformFeeAmount().doubleValue());
        dto.setPlatformFeeCharged(loan.getPlatformFeeCharged());

        dto.setDailyInterestAmount(loan.getDailyInterestAmount() == null ? null : loan.getDailyInterestAmount().doubleValue());
        dto.setTotalInterestRebateAmount(loan.getTotalInterestRebateAmount() == null ? null : loan.getTotalInterestRebateAmount().doubleValue());

        java.math.BigDecimal effectiveTotalRepayable = loan.getTotalRepayableAmount() == null
                ? java.math.BigDecimal.ZERO
                : loan.getTotalRepayableAmount().subtract(
                loan.getTotalInterestRebateAmount() == null ? java.math.BigDecimal.ZERO : loan.getTotalInterestRebateAmount()
        );

        dto.setEffectiveTotalRepayableAmount(effectiveTotalRepayable.doubleValue());

        return dto;
    }
}