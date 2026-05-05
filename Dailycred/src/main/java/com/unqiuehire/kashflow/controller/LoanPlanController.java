package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.LoanPlanRequest;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanPlanResponseDto;
import com.unqiuehire.kashflow.service.LoanPlanService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loan-plans")
@RequiredArgsConstructor
public class LoanPlanController {

    private final LoanPlanService service;
    @PostMapping("/lender/{lenderId}")
    public ApiResponse<LoanPlanResponseDto> create(
            @PathVariable Long lenderId,
            @RequestBody LoanPlanRequest request) {
        return service.createLoanPlan(lenderId, request);
    }

    // GET
    @GetMapping("/lender/{lenderId}")
    public ApiResponse<List<LoanPlanResponseDto>> getByLender(@PathVariable Long lenderId) {
        return service.getLoanPlansByLenderId(lenderId);
    }

    // UPDATE
    @PutMapping("/lender/{lenderId}/plan/{planId}")
    public ApiResponse<LoanPlanResponseDto> update(
            @PathVariable Long lenderId,
            @PathVariable Long planId,
            @RequestBody LoanPlanRequest request) {
        return service.updateLoanPlanByLenderId(lenderId, planId, request);
    }
}