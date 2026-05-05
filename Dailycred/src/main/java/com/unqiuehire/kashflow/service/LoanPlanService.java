package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.LoanPlanRequest;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanPlanResponseDto;

import java.util.List;

public interface LoanPlanService {

    ApiResponse<LoanPlanResponseDto> createLoanPlan(Long lenderId,LoanPlanRequest request);

    ApiResponse<List<LoanPlanResponseDto>> getLoanPlansByLenderId(Long lenderId);

    ApiResponse<LoanPlanResponseDto> updateLoanPlanByLenderId(Long lenderId,Long planId,LoanPlanRequest request);
//
//    ApiResponse<LoanPlanResponseDto> createLoanPlan(LoanPlanRequest request);
//
//    ApiResponse<LoanPlanResponseDto> getLoanPlanById(Long id);
//
//    ApiResponse<List<LoanPlanResponseDto>> getAllLoanPlans();
//
//    ApiResponse<LoanPlanResponseDto> updateLoanPlan(Long id, LoanPlanRequest request);
//
//    ApiResponse<String> deleteLoanPlan(Long id);
}