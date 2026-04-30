package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.dto.requestdto.AdminActionRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.service.AdminOversightService;
import com.unqiuehire.kashflow.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOversightServiceImpl implements AdminOversightService {

    private final BorrowerRepository borrowerRepository;
    private final LenderRepository lenderRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public ApiResponse<String> freezeBorrower(Long borrowerId, AdminActionRequestDto requestDto) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        borrower.setFrozen(true);
        borrower.setFreezeReason(safeReason(requestDto));
        borrowerRepository.save(borrower);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower frozen successfully", "Borrower frozen successfully");
    }

    @Override
    @Transactional
    public ApiResponse<String> unfreezeBorrower(Long borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        borrower.setFrozen(false);
        borrower.setFreezeReason(null);
        borrowerRepository.save(borrower);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower unfrozen successfully", "Borrower unfrozen successfully");
    }

    @Override
    @Transactional
    public ApiResponse<String> freezeLender(Long lenderId, AdminActionRequestDto requestDto) {
        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender not found"));

        lender.setFrozen(true);
        lender.setFreezeReason(safeReason(requestDto));
        lenderRepository.save(lender);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender frozen successfully", "Lender frozen successfully");
    }

    @Override
    @Transactional
    public ApiResponse<String> unfreezeLender(Long lenderId) {
        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender not found"));

        lender.setFrozen(false);
        lender.setFreezeReason(null);
        lenderRepository.save(lender);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender unfrozen successfully", "Lender unfrozen successfully");
    }

    @Override
    @Transactional
    public ApiResponse<String> markBorrowerFraud(Long borrowerId, AdminActionRequestDto requestDto) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        borrower.setFraudFlag(true);
        borrower.setManualReviewFlag(true);
        borrower.setFraudReason(safeReason(requestDto));
        borrowerRepository.save(borrower);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower fraud flag applied", "Borrower fraud flag applied");
    }

    @Override
    @Transactional
    public ApiResponse<String> clearBorrowerFraud(Long borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        borrower.setFraudFlag(false);
        borrower.setFraudReason(null);
        borrowerRepository.save(borrower);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower fraud flag cleared", "Borrower fraud flag cleared");
    }

    @Override
    @Transactional
    public ApiResponse<String> markLenderFraud(Long lenderId, AdminActionRequestDto requestDto) {
        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender not found"));

        lender.setFraudFlag(true);
        lender.setManualReviewFlag(true);
        lender.setFraudReason(safeReason(requestDto));
        lenderRepository.save(lender);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender fraud flag applied", "Lender fraud flag applied");
    }

    @Override
    @Transactional
    public ApiResponse<String> clearLenderFraud(Long lenderId) {
        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender not found"));

        lender.setFraudFlag(false);
        lender.setFraudReason(null);
        lenderRepository.save(lender);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender fraud flag cleared", "Lender fraud flag cleared");
    }

    @Override
    @Transactional
    public ApiResponse<String> blacklistBorrower(Long borrowerId, AdminActionRequestDto requestDto) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        borrower.setBlacklisted(true);
        borrower.setManualReviewFlag(true);
        borrower.setFraudReason(safeReason(requestDto));
        borrowerRepository.save(borrower);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower blacklisted", "Borrower blacklisted");
    }

    @Override
    @Transactional
    public ApiResponse<String> removeBorrowerBlacklist(Long borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        borrower.setBlacklisted(false);
        borrowerRepository.save(borrower);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower blacklist removed", "Borrower blacklist removed");
    }

    @Override
    @Transactional
    public ApiResponse<String> blacklistLender(Long lenderId, AdminActionRequestDto requestDto) {
        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender not found"));

        lender.setBlacklisted(true);
        lender.setManualReviewFlag(true);
        lender.setFraudReason(safeReason(requestDto));
        lenderRepository.save(lender);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender blacklisted", "Lender blacklisted");
    }

    @Override
    @Transactional
    public ApiResponse<String> removeLenderBlacklist(Long lenderId) {
        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender not found"));

        lender.setBlacklisted(false);
        lenderRepository.save(lender);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender blacklist removed", "Lender blacklist removed");
    }

    @Override
    public ApiResponse<String> freezeWallet(WalletOwnerType ownerType, Long ownerId) {
        walletService.freezeWallet(ownerType, ownerId);
        return new ApiResponse<>(ApiStatus.SUCCESS, "Wallet frozen successfully", "Wallet frozen successfully");
    }

    @Override
    public ApiResponse<String> unfreezeWallet(WalletOwnerType ownerType, Long ownerId) {
        walletService.unfreezeWallet(ownerType, ownerId);
        return new ApiResponse<>(ApiStatus.SUCCESS, "Wallet unfrozen successfully", "Wallet unfrozen successfully");
    }

    private String safeReason(AdminActionRequestDto requestDto) {
        if (requestDto == null || requestDto.getReason() == null || requestDto.getReason().trim().isEmpty()) {
            return "No reason provided";
        }
        return requestDto.getReason().trim();
    }
}