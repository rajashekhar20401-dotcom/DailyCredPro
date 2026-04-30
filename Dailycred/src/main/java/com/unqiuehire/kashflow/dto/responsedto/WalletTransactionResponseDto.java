package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.WalletTransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class WalletTransactionResponseDto {
    private Long transactionId;
    private Long walletId;
    private BigDecimal amount;
    private WalletTransactionType transactionType;
    private String description;
    private LocalDateTime transactionTime;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Long loanId;
    private Long repaymentId;
}
