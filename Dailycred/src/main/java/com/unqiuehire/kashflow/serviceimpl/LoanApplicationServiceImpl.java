package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.ApplicationStatus;
import com.unqiuehire.kashflow.dto.requestdto.LoanApplicationApprovalRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.LoanApplicationRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.LoanRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanApplicationResponseDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.entity.LoanApplication;
import com.unqiuehire.kashflow.entity.LoanPlan;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.repository.LoanApplicationRepository;
import com.unqiuehire.kashflow.repository.LoanPlanRepository;
import com.unqiuehire.kashflow.service.LoanApplicationService;
import com.unqiuehire.kashflow.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanPlanRepository loanPlanRepository;
    private final BorrowerRepository borrowerRepository;
    private final LenderRepository lenderRepository;
    private final LoanService loanService;

    @Override
    public ApiResponse<LoanApplicationResponseDto> applyLoan(Long borrowerId, Long lenderId, Long planId, LoanApplicationRequestDto requestDto) {

        Optional<Borrower> borrowerOptional = borrowerRepository.findById(borrowerId);
        if (borrowerOptional.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower not found", null);
        }

        Optional<Lender> lenderOptional = lenderRepository.findById(lenderId);
        if (lenderOptional.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender not found", null);
        }

        Optional<LoanPlan> planOptional = loanPlanRepository.findById(planId);
        if (planOptional.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan plan not found", null);
        }

        Borrower borrower = borrowerOptional.get();
        Lender lender = lenderOptional.get();
        LoanPlan loanPlan = planOptional.get();

        if (Boolean.TRUE.equals(borrower.getFrozen())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower account is frozen: " + safeReason(borrower.getFreezeReason()), null);
        }

        if (Boolean.TRUE.equals(lender.getFrozen())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender account is frozen: " + safeReason(lender.getFreezeReason()), null);
        }

        if (Boolean.TRUE.equals(borrower.getBlacklisted())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower is blacklisted", null);
        }

        if (Boolean.TRUE.equals(lender.getBlacklisted())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender is blacklisted", null);
        }

        if (!loanPlan.getLender().getLenderId().equals(lenderId)) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender mismatch with loan plan", null);
        }

        // keep age rule
        if (loanPlan.getMinAge() == null || loanPlan.getMaxAge() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan plan age not configured", null);
        }

        if (requestDto.getAge() < loanPlan.getMinAge() || requestDto.getAge() > loanPlan.getMaxAge()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Age not eligible", null);
        }

        // keep income rule
        if (requestDto.getMonthlyIncome() < loanPlan.getMinMonthlyIncome()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Income too low", null);
        }

        boolean collateralRequired = isCollateralRequiredForBorrower(borrower);

        if (collateralRequired) {
            if (requestDto.getCollateral() == null || requestDto.getCollateral().trim().isEmpty()) {
                return new ApiResponse<>(ApiStatus.FAILURE, "Collateral is required for this borrower profile", null);
            }
        }

        LoanApplication application = new LoanApplication();
        application.setBorrower(borrower);
        application.setLender(lender);
        application.setLoanPlan(loanPlan);
        application.setLoanAmount(requestDto.getLoanAmount());
        application.setAge(requestDto.getAge());
        application.setMonthlyIncome(requestDto.getMonthlyIncome());
        application.setEmploymentType(requestDto.getEmployeeType());
        application.setPinCode(requestDto.getPinCode());

        // no longer used in product flow, keep compatible defaults
        application.setIsEducated(false);
        application.setCertificates(null);

        application.setCollateral(collateralRequired ? requestDto.getCollateral() : null);
        application.setStatus(ApplicationStatus.PENDING);
        application.setAppliedAt(LocalDateTime.now());

        LoanApplication saved = loanApplicationRepository.save(application);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Loan applied successfully",
                mapToResponse(saved)
        );
    }

    @Override
    @Transactional
    public ApiResponse<LoanApplicationResponseDto> updateLoanDecision(
            Long applicationId,
            Long lenderId,
            LoanApplicationApprovalRequestDto requestDto) {

        Optional<LoanApplication> optional = loanApplicationRepository.findById(applicationId);

        if (optional.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan application not found", null);
        }

        LoanApplication application = optional.get();

        if (!application.getLender().getLenderId().equals(lenderId)) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Unauthorized: Lender mismatch", null);
        }

        if (requestDto.getApplicationStatus() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Application status is required", null);
        }

        if (application.getStatus() != ApplicationStatus.PENDING) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Application already processed", null);
        }

        if (requestDto.getApplicationStatus() == ApplicationStatus.PENDING) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Cannot set status back to PENDING", null);
        }

        application.setStatus(requestDto.getApplicationStatus());

        if (requestDto.getRemarks() != null) {
            application.setRejectionReason(requestDto.getRemarks());
        }

        application.setUpdatedAt(LocalDateTime.now());

        if (requestDto.getApplicationStatus() == ApplicationStatus.APPROVED) {

            if (!application.getIsLoanCreated()) {

                LoanRequestDto loanRequest = new LoanRequestDto();

                loanRequest.setLoanApplicationId(application.getApplicationId());
                loanRequest.setBorrowerId(application.getBorrower().getBorrowerId());
                loanRequest.setLenderId(application.getLender().getLenderId());
                loanRequest.setPlanId(application.getLoanPlan().getId());

                loanRequest.setSanctionedAmount(application.getLoanAmount());
                loanRequest.setTotalAmount(application.getLoanAmount());

                loanRequest.setTenureDays(application.getLoanPlan().getPlanDuration());
                loanRequest.setInterestPerDay(application.getLoanPlan().getInterestPerDay());
                loanRequest.setPenaltyAmount(application.getLoanPlan().getPenaltyAmount());

                loanRequest.setStartDate(LocalDate.now());

                ApiResponse<?> loanCreationResponse = loanService.createLoan(loanRequest);

                if (loanCreationResponse.getStatus() == ApiStatus.FAILURE) {
                    throw new RuntimeException("Loan creation failed: " + loanCreationResponse.getMessage());
                }

                application.setIsLoanCreated(true);
            }
        }

        LoanApplication updated = loanApplicationRepository.save(application);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Loan application decision updated successfully",
                mapToResponse(updated)
        );
    }

    @Override
    public ApiResponse<LoanApplicationResponseDto> getApplicationById(Long applicationId) {

        Optional<LoanApplication> optional = loanApplicationRepository.findById(applicationId);

        if (optional.isEmpty()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Loan application not found",
                    null
            );
        }

        LoanApplication application = optional.get();

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Loan application fetched successfully",
                mapToResponse(application)
        );
    }

    @Override
    public ApiResponse<List<LoanApplicationResponseDto>> getApplicationsByLenderId(Long lenderId) {

        List<LoanApplicationResponseDto> list = loanApplicationRepository
                .findByLender_LenderId(lenderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Lender applications fetched successfully",
                list
        );
    }

    @Override
    public ApiResponse<List<LoanApplicationResponseDto>> getApplicationsByBorrowerId(Long borrowerId) {

        List<LoanApplicationResponseDto> list = loanApplicationRepository
                .findByBorrower_BorrowerId(borrowerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Borrower applications fetched successfully",
                list
        );
    }

    private LoanApplicationResponseDto mapToResponse(LoanApplication application) {
        LoanApplicationResponseDto dto = new LoanApplicationResponseDto();
        dto.setApplicationId(application.getApplicationId());
        dto.setBorrowerId(application.getBorrower().getBorrowerId());
        dto.setLenderId(application.getLender().getLenderId());
        dto.setPlanId(application.getLoanPlan().getId());
        dto.setLoanAmount(application.getLoanAmount());
        dto.setAge(application.getAge());
        dto.setMonthlyIncome(application.getMonthlyIncome());
        dto.setEmployeeType(application.getEmploymentType());
        dto.setPinCode(application.getPinCode());
        dto.setIsEducated(application.getIsEducated());
        dto.setCertificates(application.getCertificates());
        dto.setCollateral(application.getCollateral());
        dto.setApplicationStatus(application.getStatus());
        dto.setAppliedAt(application.getAppliedAt());
        return dto;
    }

    private boolean isCollateralRequiredForBorrower(Borrower borrower) {
        String riskCategory = borrower.getRiskCategory() == null ? "" : borrower.getRiskCategory().trim().toUpperCase();
        String eligibilityStatus = borrower.getEligibilityStatus() == null ? "" : borrower.getEligibilityStatus().trim().toUpperCase();

        int totalLoansTaken = borrower.getTotalLoansTaken() == null ? 0 : borrower.getTotalLoansTaken();
        int defaultedLoanCount = borrower.getDefaultedLoanCount() == null ? 0 : borrower.getDefaultedLoanCount();
        int totalMissedDays = borrower.getTotalMissedDays() == null ? 0 : borrower.getTotalMissedDays();
        int maxConsecutiveMissedDays = borrower.getMaxConsecutiveMissedDays() == null ? 0 : borrower.getMaxConsecutiveMissedDays();

        boolean highRisk = "HIGH".equals(riskCategory);
        boolean explicitlyCollateralRequired = "COLLATERAL_REQUIRED".equals(eligibilityStatus);

        boolean poorEarlyHistory =
                totalLoansTaken > 0
                        && totalLoansTaken < 3
                        && (
                        defaultedLoanCount >= totalLoansTaken
                                || totalMissedDays >= 10
                                || maxConsecutiveMissedDays >= 5
                );

        return highRisk || explicitlyCollateralRequired || poorEarlyHistory;
    }

    private String safeReason(String reason) {
        return (reason == null || reason.trim().isEmpty()) ? "no reason provided" : reason;
    }
}