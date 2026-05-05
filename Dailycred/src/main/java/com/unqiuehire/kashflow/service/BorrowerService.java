package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.BorrowerRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerResponseDto;

import java.util.List;

public interface BorrowerService {

    ApiResponse<BorrowerResponseDto> createBorrower(BorrowerRequestDto borrowerRequestDto);

    ApiResponse<BorrowerResponseDto> getBorrowerById(Long borrowerId);

    ApiResponse<List<BorrowerResponseDto>> getAllBorrowers();

    ApiResponse<BorrowerResponseDto> updateBorrower(Long borrowerId, BorrowerRequestDto borrowerRequestDto);

    ApiResponse<String> deleteBorrower(Long borrowerId);
}