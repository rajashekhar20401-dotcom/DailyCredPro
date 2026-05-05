package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.LoanRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanResponseDto;

import java.util.List;

public interface LoanService {

    ApiResponse<LoanResponseDto> createLoan(LoanRequestDto dto);

    ApiResponse<LoanResponseDto> getLoanById(Long loanId);

    ApiResponse<List<LoanResponseDto>> getLoansByBorrower(Long borrowerId);

    ApiResponse<List<LoanResponseDto>> getLoansByLender(Long lenderId);

    ApiResponse<String> closeLoan(Long loanId);
}