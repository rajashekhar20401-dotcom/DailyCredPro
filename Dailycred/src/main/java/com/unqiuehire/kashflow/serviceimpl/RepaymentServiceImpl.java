package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.PaymentStatus;
import com.unqiuehire.kashflow.dto.requestdto.RepaymentRequestDTO;
import com.unqiuehire.kashflow.dto.responsedto.RepaymentResponseDTO;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.LoanApplication;
import com.unqiuehire.kashflow.entity.Repayment;
import com.unqiuehire.kashflow.repository.LoanApplicationRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.repository.RepaymentRepository;
import com.unqiuehire.kashflow.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public RepaymentResponseDTO makePayment(RepaymentRequestDTO request) {

        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        LoanApplication loanApplication = loanApplicationRepository.findById(request.getLoanApplicationId())
                .orElseThrow(() -> new RuntimeException("Loan Application not found"));

        //  VALIDATION
        if (!loan.getLoanApplicationId().equals(request.getLoanApplicationId())) {
            throw new RuntimeException("Loan mismatch");
        }

        double amount = request.getAmountPaid();
        double remaining = loan.getRemainingAmount();

        //  BUSINESS LOGIC

        boolean isPartial = amount < loan.getDailyEmi();
        boolean isMissed = request.getMissedDays() != null && request.getMissedDays() > 0;
        boolean isEarly = amount > loan.getDailyEmi();

        double interest = 0;
        double penalty = 0;

        if (isMissed) {
            interest = loan.getInterestPerDay() * request.getMissedDays();
            penalty = loan.getPenaltyAmount() * request.getMissedDays();
        }

        double newRemaining = remaining - amount + interest + penalty;

        if (newRemaining < 0) {
            throw new RuntimeException("Over payment not allowed");
        }

        loan.setRemainingAmount(newRemaining);

        if (newRemaining == 0) {
            loan.setIsClosed(true);
        }

        loanRepository.save(loan);

        //  Save repayment
        Repayment repayment = Repayment.builder()
                .loan(loan)
                .loanApplication(loanApplication)
                .amountPaid(amount)
                .paymentDate(LocalDate.now())
                .paymentMode(request.getPaymentMode())
                .paymentStatus(PaymentStatus.SUCCESS)
                .transactionReference(UUID.randomUUID().toString())

                .isPartialPayment(isPartial)
                .isEarlyPayment(isEarly)
                .isMissedPayment(isMissed)

                .interestAdded(interest)
                .penaltyAmount(penalty)
                .missedDays(request.getMissedDays())

                .balanceAmount(newRemaining)
                .build();

        repaymentRepository.save(repayment);

        return mapToDTO(repayment);
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

    private RepaymentResponseDTO mapToDTO(Repayment r) {
        return RepaymentResponseDTO.builder()
                .id(r.getId())
                .amountPaid(r.getAmountPaid())
                .paymentDate(r.getPaymentDate())
                .paymentMode(r.getPaymentMode())
                .paymentStatus(r.getPaymentStatus())

                .isPartialPayment(r.getIsPartialPayment())
                .isEarlyPayment(r.getIsEarlyPayment())
                .isMissedPayment(r.getIsMissedPayment())

                .interestAdded(r.getInterestAdded())
                .penaltyAmount(r.getPenaltyAmount())
                .missedDays(r.getMissedDays())

                .balanceAmount(r.getBalanceAmount())

                .transactionReference(r.getTransactionReference())
                .build();
    }
}