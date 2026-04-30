package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.constant.WalletTransactionType;
import com.unqiuehire.kashflow.dto.responsedto.WalletResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.WalletTransactionResponseDto;
import com.unqiuehire.kashflow.entity.Wallet;
import com.unqiuehire.kashflow.entity.WalletTransaction;
import com.unqiuehire.kashflow.repository.WalletRepository;
import com.unqiuehire.kashflow.repository.WalletTransactionRepository;
import com.unqiuehire.kashflow.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public Wallet getOrCreateWallet(WalletOwnerType walletOwnerType, Long ownerId) {
        return walletRepository.findByWalletOwnerTypeAndOwnerId(walletOwnerType, ownerId)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setWalletOwnerType(walletOwnerType);
                    wallet.setOwnerId(ownerId);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setActive(true);
                    wallet.setFrozen(false);
                    return walletRepository.save(wallet);
                });
    }

    @Override
    public WalletResponseDto getWallet(WalletOwnerType walletOwnerType, Long ownerId) {
        return mapToResponse(getOrCreateWallet(walletOwnerType, ownerId));
    }

    @Override
    public List<WalletTransactionResponseDto> getWalletTransactions(WalletOwnerType walletOwnerType, Long ownerId) {
        Wallet wallet = getOrCreateWallet(walletOwnerType, ownerId);

        return walletTransactionRepository.findByWalletId(wallet.getWalletId())
                .stream()
                .map(this::mapTxn)
                .toList();
    }

    @Override
    @Transactional
    public WalletResponseDto topupWallet(WalletOwnerType walletOwnerType, Long ownerId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Topup amount must be greater than zero");
        }

        Wallet wallet = getOrCreateWallet(walletOwnerType, ownerId);

        if (Boolean.TRUE.equals(wallet.getFrozen())) {
            throw new RuntimeException("Wallet is frozen");
        }

        creditWallet(
                walletOwnerType,
                ownerId,
                amount,
                WalletTransactionType.MANUAL_TOPUP,
                description == null ? "Manual wallet topup" : description,
                null,
                null
        );

        return getWallet(walletOwnerType, ownerId);
    }

    @Override
    @Transactional
    public WalletResponseDto withdrawWallet(WalletOwnerType walletOwnerType, Long ownerId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdraw amount must be greater than zero");
        }

        Wallet wallet = getOrCreateWallet(walletOwnerType, ownerId);

        if (Boolean.TRUE.equals(wallet.getFrozen())) {
            throw new RuntimeException("Wallet is frozen");
        }

        debitWallet(
                walletOwnerType,
                ownerId,
                amount,
                WalletTransactionType.ADJUSTMENT,
                description == null ? "Manual wallet withdrawal" : description,
                null,
                null
        );

        return getWallet(walletOwnerType, ownerId);
    }

    @Override
    @Transactional
    public WalletResponseDto freezeWallet(WalletOwnerType walletOwnerType, Long ownerId) {
        Wallet wallet = getOrCreateWallet(walletOwnerType, ownerId);
        wallet.setFrozen(true);
        walletRepository.save(wallet);
        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponseDto unfreezeWallet(WalletOwnerType walletOwnerType, Long ownerId) {
        Wallet wallet = getOrCreateWallet(walletOwnerType, ownerId);
        wallet.setFrozen(false);
        walletRepository.save(wallet);
        return mapToResponse(wallet);
    }

    @Override
    public void creditWallet(
            WalletOwnerType walletOwnerType,
            Long ownerId,
            BigDecimal amount,
            WalletTransactionType transactionType,
            String description,
            Long loanId,
            Long repaymentId
    ) {
        Wallet wallet = getOrCreateWallet(walletOwnerType, ownerId);

        if (Boolean.TRUE.equals(wallet.getFrozen())) {
            throw new RuntimeException("Wallet is frozen");
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);

        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setWalletId(wallet.getWalletId());
        txn.setAmount(amount);
        txn.setTransactionType(transactionType);
        txn.setDescription(description);
        txn.setTransactionTime(LocalDateTime.now());
        txn.setBalanceBefore(before);
        txn.setBalanceAfter(after);
        txn.setLoanId(loanId);
        txn.setRepaymentId(repaymentId);

        walletTransactionRepository.save(txn);
    }

    @Override
    public void debitWallet(
            WalletOwnerType walletOwnerType,
            Long ownerId,
            BigDecimal amount,
            WalletTransactionType transactionType,
            String description,
            Long loanId,
            Long repaymentId
    ) {
        Wallet wallet = getOrCreateWallet(walletOwnerType, ownerId);

        if (Boolean.TRUE.equals(wallet.getFrozen())) {
            throw new RuntimeException("Wallet is frozen");
        }

        BigDecimal before = wallet.getBalance();

        if (before.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        BigDecimal after = before.subtract(amount);

        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setWalletId(wallet.getWalletId());
        txn.setAmount(amount);
        txn.setTransactionType(transactionType);
        txn.setDescription(description);
        txn.setTransactionTime(LocalDateTime.now());
        txn.setBalanceBefore(before);
        txn.setBalanceAfter(after);
        txn.setLoanId(loanId);
        txn.setRepaymentId(repaymentId);

        walletTransactionRepository.save(txn);
    }

    private WalletResponseDto mapToResponse(Wallet wallet) {
        WalletResponseDto dto = new WalletResponseDto();
        dto.setWalletId(wallet.getWalletId());
        dto.setWalletOwnerType(wallet.getWalletOwnerType());
        dto.setOwnerId(wallet.getOwnerId());
        dto.setBalance(wallet.getBalance());
        dto.setActive(wallet.getActive());
        dto.setFrozen(wallet.getFrozen());
        return dto;
    }

    private WalletTransactionResponseDto mapTxn(WalletTransaction txn) {
        WalletTransactionResponseDto dto = new WalletTransactionResponseDto();
        dto.setTransactionId(txn.getTransactionId());
        dto.setWalletId(txn.getWalletId());
        dto.setAmount(txn.getAmount());
        dto.setTransactionType(txn.getTransactionType());
        dto.setDescription(txn.getDescription());
        dto.setTransactionTime(txn.getTransactionTime());
        dto.setBalanceBefore(txn.getBalanceBefore());
        dto.setBalanceAfter(txn.getBalanceAfter());
        dto.setLoanId(txn.getLoanId());
        dto.setRepaymentId(txn.getRepaymentId());
        return dto;
    }
}