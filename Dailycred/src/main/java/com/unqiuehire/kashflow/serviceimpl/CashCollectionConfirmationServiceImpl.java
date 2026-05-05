package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.CashCollectionStatus;
import com.unqiuehire.kashflow.constant.PaymentMode;
import com.unqiuehire.kashflow.dto.requestdto.CashCollectionConfirmRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.CashCollectionInitiateRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.CashCollectionRejectRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.RepaymentRequestDTO;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.CashCollectionConfirmationResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.RepaymentResponseDTO;
import com.unqiuehire.kashflow.entity.CashCollectionConfirmation;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.repository.CashCollectionConfirmationRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.service.CashCollectionConfirmationService;
import com.unqiuehire.kashflow.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashCollectionConfirmationServiceImpl implements CashCollectionConfirmationService {

    private final CashCollectionConfirmationRepository repository;
    private final LoanRepository loanRepository;
    private final RepaymentService repaymentService;

    @Override
    @Transactional
    public ApiResponse<CashCollectionConfirmationResponseDto> initiateCashCollection(Long lenderId, CashCollectionInitiateRequestDto requestDto) {

        if (requestDto == null || requestDto.getLoanId() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan id is required", null);
        }

        if (requestDto.getAmount() == null || requestDto.getAmount() <= 0) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Cash collection amount must be greater than zero", null);
        }

        Loan loan = loanRepository.findById(requestDto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getLenderId().equals(lenderId)) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan does not belong to this lender", null);
        }

        if (Boolean.TRUE.equals(loan.getIsClosed())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan is already closed", null);
        }

        CashCollectionConfirmation confirmation = new CashCollectionConfirmation();
        confirmation.setLoanId(loan.getLoanId());
        confirmation.setLoanApplicationId(loan.getLoanApplicationId());
        confirmation.setLenderId(loan.getLenderId());
        confirmation.setBorrowerId(loan.getBorrowerId());
        confirmation.setAmount(BigDecimal.valueOf(requestDto.getAmount()));
        confirmation.setPaymentDate(requestDto.getPaymentDate() == null ? LocalDate.now() : requestDto.getPaymentDate());
        confirmation.setGeneratedToken(generateSixDigitToken());
        confirmation.setStatus(CashCollectionStatus.PENDING_BORROWER_CONFIRMATION);
        confirmation.setLenderNote(safeText(requestDto.getLenderNote()));
        confirmation.setCreatedAt(LocalDateTime.now());
        confirmation.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        CashCollectionConfirmation saved = repository.save(confirmation);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Cash collection initiated successfully", mapToResponse(saved));
    }

    @Override
    @Transactional
    public ApiResponse<CashCollectionConfirmationResponseDto> confirmCashCollection(Long borrowerId, Long confirmationId, CashCollectionConfirmRequestDto requestDto) {

        CashCollectionConfirmation confirmation = repository.findById(confirmationId)
                .orElseThrow(() -> new RuntimeException("Cash collection confirmation not found"));

        if (!confirmation.getBorrowerId().equals(borrowerId)) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Unauthorized borrower for this confirmation", null);
        }

        if (confirmation.getStatus() != CashCollectionStatus.PENDING_BORROWER_CONFIRMATION) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Cash collection request is not pending", null);
        }

        if (confirmation.getExpiresAt() != null && LocalDateTime.now().isAfter(confirmation.getExpiresAt())) {
            confirmation.setStatus(CashCollectionStatus.EXPIRED);
            repository.save(confirmation);
            return new ApiResponse<>(ApiStatus.FAILURE, "Cash collection token expired", null);
        }

        String providedToken = requestDto == null ? null : safeText(requestDto.getConfirmationToken());
        if (providedToken == null || !providedToken.equals(confirmation.getGeneratedToken())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Invalid confirmation token", null);
        }

        RepaymentRequestDTO repaymentRequestDTO = new RepaymentRequestDTO();
        repaymentRequestDTO.setLoanId(confirmation.getLoanId());
        repaymentRequestDTO.setLoanApplicationId(confirmation.getLoanApplicationId());
        repaymentRequestDTO.setAmountPaid(confirmation.getAmount().doubleValue());
        repaymentRequestDTO.setPaymentMode(PaymentMode.CASH);
        repaymentRequestDTO.setPaymentDate(confirmation.getPaymentDate());

        RepaymentResponseDTO repaymentResponseDTO = repaymentService.makePayment(repaymentRequestDTO);

        confirmation.setStatus(CashCollectionStatus.CONFIRMED);
        confirmation.setBorrowerNote(requestDto == null ? null : safeText(requestDto.getBorrowerNote()));
        confirmation.setConfirmedAt(LocalDateTime.now());
        confirmation.setRepaymentId(repaymentResponseDTO.getId());

        CashCollectionConfirmation updated = repository.save(confirmation);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Cash collection confirmed successfully", mapToResponse(updated));
    }

    @Override
    @Transactional
    public ApiResponse<CashCollectionConfirmationResponseDto> rejectCashCollection(Long borrowerId, Long confirmationId, CashCollectionRejectRequestDto requestDto) {

        CashCollectionConfirmation confirmation = repository.findById(confirmationId)
                .orElseThrow(() -> new RuntimeException("Cash collection confirmation not found"));

        if (!confirmation.getBorrowerId().equals(borrowerId)) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Unauthorized borrower for this confirmation", null);
        }

        if (confirmation.getStatus() != CashCollectionStatus.PENDING_BORROWER_CONFIRMATION) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Cash collection request is not pending", null);
        }

        if (confirmation.getExpiresAt() != null && LocalDateTime.now().isAfter(confirmation.getExpiresAt())) {
            confirmation.setStatus(CashCollectionStatus.EXPIRED);
            repository.save(confirmation);
            return new ApiResponse<>(ApiStatus.FAILURE, "Cash collection token expired", null);
        }

        confirmation.setStatus(CashCollectionStatus.REJECTED);
        confirmation.setBorrowerNote(requestDto == null ? null : safeText(requestDto.getBorrowerNote()));
        confirmation.setRejectedAt(LocalDateTime.now());

        CashCollectionConfirmation updated = repository.save(confirmation);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Cash collection rejected successfully", mapToResponse(updated));
    }

    @Override
    public ApiResponse<List<CashCollectionConfirmationResponseDto>> getByLender(Long lenderId) {
        List<CashCollectionConfirmationResponseDto> list = repository.findByLenderIdOrderByCreatedAtDesc(lenderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(ApiStatus.SUCCESS, "Cash collection requests fetched for lender", list);
    }

    @Override
    public ApiResponse<List<CashCollectionConfirmationResponseDto>> getByBorrower(Long borrowerId) {
        List<CashCollectionConfirmationResponseDto> list = repository.findByBorrowerIdOrderByCreatedAtDesc(borrowerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(ApiStatus.SUCCESS, "Cash collection requests fetched for borrower", list);
    }

    private CashCollectionConfirmationResponseDto mapToResponse(CashCollectionConfirmation confirmation) {
        CashCollectionConfirmationResponseDto dto = new CashCollectionConfirmationResponseDto();
        dto.setConfirmationId(confirmation.getConfirmationId());
        dto.setLoanId(confirmation.getLoanId());
        dto.setLoanApplicationId(confirmation.getLoanApplicationId());
        dto.setLenderId(confirmation.getLenderId());
        dto.setBorrowerId(confirmation.getBorrowerId());
        dto.setAmount(confirmation.getAmount());
        dto.setPaymentDate(confirmation.getPaymentDate());
        dto.setGeneratedToken(confirmation.getGeneratedToken());
        dto.setStatus(confirmation.getStatus());
        dto.setLenderNote(confirmation.getLenderNote());
        dto.setBorrowerNote(confirmation.getBorrowerNote());
        dto.setCreatedAt(confirmation.getCreatedAt());
        dto.setExpiresAt(confirmation.getExpiresAt());
        dto.setConfirmedAt(confirmation.getConfirmedAt());
        dto.setRejectedAt(confirmation.getRejectedAt());
        dto.setRepaymentId(confirmation.getRepaymentId());
        return dto;
    }

    private String generateSixDigitToken() {
        int token = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(token);
    }

    private String safeText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}