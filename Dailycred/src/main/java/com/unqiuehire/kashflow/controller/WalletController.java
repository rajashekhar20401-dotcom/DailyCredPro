package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.dto.requestdto.WalletTopupRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.WalletWithdrawRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.WalletResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.WalletTransactionResponseDto;
import com.unqiuehire.kashflow.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{ownerType}/{ownerId}")
    public WalletResponseDto getWallet(@PathVariable WalletOwnerType ownerType, @PathVariable Long ownerId) {
        return walletService.getWallet(ownerType, ownerId);
    }

    @GetMapping("/{ownerType}/{ownerId}/transactions")
    public List<WalletTransactionResponseDto> getWalletTransactions(@PathVariable WalletOwnerType ownerType, @PathVariable Long ownerId) {
        return walletService.getWalletTransactions(ownerType, ownerId);
    }

    @PostMapping("/topup")
    public WalletResponseDto topupWallet(@RequestBody WalletTopupRequestDto request) {
        return walletService.topupWallet(
                request.getWalletOwnerType(),
                request.getOwnerId(),
                request.getAmount(),
                request.getDescription()
        );
    }

    @PostMapping("/withdraw")
    public WalletResponseDto withdrawWallet(@RequestBody WalletWithdrawRequestDto request) {
        return walletService.withdrawWallet(
                request.getWalletOwnerType(),
                request.getOwnerId(),
                request.getAmount(),
                request.getDescription()
        );
    }

    @PostMapping("/{ownerType}/{ownerId}/freeze")
    public WalletResponseDto freezeWallet(@PathVariable WalletOwnerType ownerType, @PathVariable Long ownerId) {
        return walletService.freezeWallet(ownerType, ownerId);
    }

    @PostMapping("/{ownerType}/{ownerId}/unfreeze")
    public WalletResponseDto unfreezeWallet(@PathVariable WalletOwnerType ownerType, @PathVariable Long ownerId) {
        return walletService.unfreezeWallet(ownerType, ownerId);
    }
}
