package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.dto.requestdto.AdminActionRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;

public interface AdminOversightService {

    ApiResponse<String> freezeBorrower(Long borrowerId, AdminActionRequestDto requestDto);
    ApiResponse<String> unfreezeBorrower(Long borrowerId);

    ApiResponse<String> freezeLender(Long lenderId, AdminActionRequestDto requestDto);
    ApiResponse<String> unfreezeLender(Long lenderId);

    ApiResponse<String> markBorrowerFraud(Long borrowerId, AdminActionRequestDto requestDto);
    ApiResponse<String> clearBorrowerFraud(Long borrowerId);

    ApiResponse<String> markLenderFraud(Long lenderId, AdminActionRequestDto requestDto);
    ApiResponse<String> clearLenderFraud(Long lenderId);

    ApiResponse<String> blacklistBorrower(Long borrowerId, AdminActionRequestDto requestDto);
    ApiResponse<String> removeBorrowerBlacklist(Long borrowerId);

    ApiResponse<String> blacklistLender(Long lenderId, AdminActionRequestDto requestDto);
    ApiResponse<String> removeLenderBlacklist(Long lenderId);

    ApiResponse<String> freezeWallet(WalletOwnerType ownerType, Long ownerId);
    ApiResponse<String> unfreezeWallet(WalletOwnerType ownerType, Long ownerId);
}