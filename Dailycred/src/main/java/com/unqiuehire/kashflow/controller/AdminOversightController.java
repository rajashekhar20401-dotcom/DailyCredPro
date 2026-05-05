package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.dto.requestdto.AdminActionRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.service.AdminOversightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/oversight")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminOversightController {

    private final AdminOversightService adminOversightService;

    @PostMapping("/borrowers/{borrowerId}/freeze")
    public ApiResponse<String> freezeBorrower(@PathVariable Long borrowerId, @RequestBody(required = false) AdminActionRequestDto requestDto) {
        return adminOversightService.freezeBorrower(borrowerId, requestDto);
    }

    @PostMapping("/borrowers/{borrowerId}/unfreeze")
    public ApiResponse<String> unfreezeBorrower(@PathVariable Long borrowerId) {
        return adminOversightService.unfreezeBorrower(borrowerId);
    }

    @PostMapping("/lenders/{lenderId}/freeze")
    public ApiResponse<String> freezeLender(@PathVariable Long lenderId, @RequestBody(required = false) AdminActionRequestDto requestDto) {
        return adminOversightService.freezeLender(lenderId, requestDto);
    }

    @PostMapping("/lenders/{lenderId}/unfreeze")
    public ApiResponse<String> unfreezeLender(@PathVariable Long lenderId) {
        return adminOversightService.unfreezeLender(lenderId);
    }

    @PostMapping("/borrowers/{borrowerId}/fraud-flag")
    public ApiResponse<String> markBorrowerFraud(@PathVariable Long borrowerId, @RequestBody(required = false) AdminActionRequestDto requestDto) {
        return adminOversightService.markBorrowerFraud(borrowerId, requestDto);
    }

    @PostMapping("/borrowers/{borrowerId}/clear-fraud-flag")
    public ApiResponse<String> clearBorrowerFraud(@PathVariable Long borrowerId) {
        return adminOversightService.clearBorrowerFraud(borrowerId);
    }

    @PostMapping("/lenders/{lenderId}/fraud-flag")
    public ApiResponse<String> markLenderFraud(@PathVariable Long lenderId, @RequestBody(required = false) AdminActionRequestDto requestDto) {
        return adminOversightService.markLenderFraud(lenderId, requestDto);
    }

    @PostMapping("/lenders/{lenderId}/clear-fraud-flag")
    public ApiResponse<String> clearLenderFraud(@PathVariable Long lenderId) {
        return adminOversightService.clearLenderFraud(lenderId);
    }

    @PostMapping("/borrowers/{borrowerId}/blacklist")
    public ApiResponse<String> blacklistBorrower(@PathVariable Long borrowerId, @RequestBody(required = false) AdminActionRequestDto requestDto) {
        return adminOversightService.blacklistBorrower(borrowerId, requestDto);
    }

    @PostMapping("/borrowers/{borrowerId}/remove-blacklist")
    public ApiResponse<String> removeBorrowerBlacklist(@PathVariable Long borrowerId) {
        return adminOversightService.removeBorrowerBlacklist(borrowerId);
    }

    @PostMapping("/lenders/{lenderId}/blacklist")
    public ApiResponse<String> blacklistLender(@PathVariable Long lenderId, @RequestBody(required = false) AdminActionRequestDto requestDto) {
        return adminOversightService.blacklistLender(lenderId, requestDto);
    }

    @PostMapping("/lenders/{lenderId}/remove-blacklist")
    public ApiResponse<String> removeLenderBlacklist(@PathVariable Long lenderId) {
        return adminOversightService.removeLenderBlacklist(lenderId);
    }

    @PostMapping("/wallets/{ownerType}/{ownerId}/freeze")
    public ApiResponse<String> freezeWallet(@PathVariable WalletOwnerType ownerType, @PathVariable Long ownerId) {
        return adminOversightService.freezeWallet(ownerType, ownerId);
    }

    @PostMapping("/wallets/{ownerType}/{ownerId}/unfreeze")
    public ApiResponse<String> unfreezeWallet(@PathVariable WalletOwnerType ownerType, @PathVariable Long ownerId) {
        return adminOversightService.unfreezeWallet(ownerType, ownerId);
    }

    @PostMapping("/admins/{adminId}/global-borrower-policy")
    public ApiResponse<String> updateGlobalBorrowerPolicy(
            @PathVariable Long adminId,
            @RequestBody AdminActionRequestDto requestDto
    ) {
        return adminOversightService.updateGlobalBorrowerPolicy(adminId, requestDto);
    }

    @PostMapping("/admins/{adminId}/borrowers/{borrowerId}/override")
    public ApiResponse<String> updateBorrowerOverride(
            @PathVariable Long adminId,
            @PathVariable Long borrowerId,
            @RequestBody AdminActionRequestDto requestDto
    ) {
        return adminOversightService.updateBorrowerOverride(adminId, borrowerId, requestDto);
    }

    @PostMapping("/admins/{adminId}/borrowers/{borrowerId}/clear-override")
    public ApiResponse<String> clearBorrowerOverride(
            @PathVariable Long adminId,
            @PathVariable Long borrowerId
    ) {
        return adminOversightService.clearBorrowerOverride(adminId, borrowerId);
    }
}