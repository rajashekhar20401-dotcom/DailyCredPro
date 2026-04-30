package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.CashCollectionConfirmRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.CashCollectionInitiateRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.CashCollectionRejectRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.CashCollectionConfirmationResponseDto;

import java.util.List;

public interface CashCollectionConfirmationService {

    ApiResponse<CashCollectionConfirmationResponseDto> initiateCashCollection(Long lenderId, CashCollectionInitiateRequestDto requestDto);

    ApiResponse<CashCollectionConfirmationResponseDto> confirmCashCollection(Long borrowerId, Long confirmationId, CashCollectionConfirmRequestDto requestDto);

    ApiResponse<CashCollectionConfirmationResponseDto> rejectCashCollection(Long borrowerId, Long confirmationId, CashCollectionRejectRequestDto requestDto);

    ApiResponse<List<CashCollectionConfirmationResponseDto>> getByLender(Long lenderId);

    ApiResponse<List<CashCollectionConfirmationResponseDto>> getByBorrower(Long borrowerId);
}