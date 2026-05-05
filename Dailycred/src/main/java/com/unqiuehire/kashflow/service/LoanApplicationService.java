package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.LoanApplicationApprovalRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.LoanApplicationRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanApplicationResponseDto;

import java.util.List;

public interface LoanApplicationService {
    ApiResponse<LoanApplicationResponseDto> applyLoan(
            Long borrowerId,
            Long lenderId,
            Long planId,
            LoanApplicationRequestDto requestDto
    );
    ApiResponse<LoanApplicationResponseDto> updateLoanDecision(
            Long applicationId,
            Long lenderId,
            LoanApplicationApprovalRequestDto requestDto
    );
//
    ApiResponse<LoanApplicationResponseDto> getApplicationById(Long applicationId);

    ApiResponse<List<LoanApplicationResponseDto>> getApplicationsByLenderId(Long lenderId);

    ApiResponse<List<LoanApplicationResponseDto>> getApplicationsByBorrowerId(Long borrowerId);

//    ApiResponse<LoanApplicationResponseDto> createApplication(LoanApplicationRequestDto dto);
//
//    ApiResponse<LoanApplicationResponseDto> approveOrRejectApplication(
//            Long applicationId,
//            LoanApplicationApprovalRequestDto requestDto
//    );

//    ApiResponse<LoanApplicationResponseDto> getById(Long id);
//
//    ApiResponse<List<LoanApplicationResponseDto>> getByBorrower(Long borrowerId);
//
//    ApiResponse<List<LoanApplicationResponseDto>> getByLender(Long lenderId);
//
//    ApiResponse<String> cancelApplication(Long id);
}