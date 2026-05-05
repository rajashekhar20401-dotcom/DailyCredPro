package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.CashCollectionConfirmRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.CashCollectionInitiateRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.CashCollectionRejectRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.CashCollectionConfirmationResponseDto;
import com.unqiuehire.kashflow.service.CashCollectionConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cash-collections")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CashCollectionConfirmationController {

    private final CashCollectionConfirmationService service;

    @PostMapping("/lender/{lenderId}/initiate")
    public ApiResponse<CashCollectionConfirmationResponseDto> initiateCashCollection(
            @PathVariable Long lenderId,
            @RequestBody CashCollectionInitiateRequestDto requestDto
    ) {
        return service.initiateCashCollection(lenderId, requestDto);
    }

    @PostMapping("/borrower/{borrowerId}/{confirmationId}/confirm")
    public ApiResponse<CashCollectionConfirmationResponseDto> confirmCashCollection(
            @PathVariable Long borrowerId,
            @PathVariable Long confirmationId,
            @RequestBody CashCollectionConfirmRequestDto requestDto
    ) {
        return service.confirmCashCollection(borrowerId, confirmationId, requestDto);
    }

    @PostMapping("/borrower/{borrowerId}/{confirmationId}/reject")
    public ApiResponse<CashCollectionConfirmationResponseDto> rejectCashCollection(
            @PathVariable Long borrowerId,
            @PathVariable Long confirmationId,
            @RequestBody CashCollectionRejectRequestDto requestDto
    ) {
        return service.rejectCashCollection(borrowerId, confirmationId, requestDto);
    }

    @GetMapping("/lender/{lenderId}")
    public ApiResponse<List<CashCollectionConfirmationResponseDto>> getByLender(@PathVariable Long lenderId) {
        return service.getByLender(lenderId);
    }

    @GetMapping("/borrower/{borrowerId}")
    public ApiResponse<List<CashCollectionConfirmationResponseDto>> getByBorrower(@PathVariable Long borrowerId) {
        return service.getByBorrower(borrowerId);
    }
}