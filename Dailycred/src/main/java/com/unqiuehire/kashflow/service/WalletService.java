package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.constant.WalletTransactionType;
import com.unqiuehire.kashflow.dto.responsedto.WalletResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.WalletTransactionResponseDto;
import com.unqiuehire.kashflow.entity.Wallet;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    Wallet getOrCreateWallet(WalletOwnerType walletOwnerType, Long ownerId);

    WalletResponseDto getWallet(WalletOwnerType walletOwnerType, Long ownerId);

    List<WalletTransactionResponseDto> getWalletTransactions(WalletOwnerType walletOwnerType, Long ownerId);

    WalletResponseDto topupWallet(
            WalletOwnerType walletOwnerType,
            Long ownerId,
            BigDecimal amount,
            String description
    );

    WalletResponseDto withdrawWallet(
            WalletOwnerType walletOwnerType,
            Long ownerId,
            BigDecimal amount,
            String description
    );

    WalletResponseDto freezeWallet(WalletOwnerType walletOwnerType, Long ownerId);

    WalletResponseDto unfreezeWallet(WalletOwnerType walletOwnerType, Long ownerId);

    void creditWallet(
            WalletOwnerType walletOwnerType,
            Long ownerId,
            BigDecimal amount,
            WalletTransactionType transactionType,
            String description,
            Long loanId,
            Long repaymentId
    );

    void debitWallet(
            WalletOwnerType walletOwnerType,
            Long ownerId,
            BigDecimal amount,
            WalletTransactionType transactionType,
            String description,
            Long loanId,
            Long repaymentId
    );
}