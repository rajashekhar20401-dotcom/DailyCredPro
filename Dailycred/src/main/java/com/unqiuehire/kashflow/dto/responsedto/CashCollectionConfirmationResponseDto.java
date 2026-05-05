package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.CashCollectionStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CashCollectionConfirmationResponseDto {
    private Long confirmationId;
    private Long loanId;
    private Long loanApplicationId;
    private Long lenderId;
    private Long borrowerId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String generatedToken;
    private CashCollectionStatus status;
    private String lenderNote;
    private String borrowerNote;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime rejectedAt;
    private Long repaymentId;
}