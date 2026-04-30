package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.LoanApplicationApprovalRequestDto;
import com.unqiuehire.kashflow.dto.requestdto.LoanApplicationRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanApplicationResponseDto;
import com.unqiuehire.kashflow.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-application")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoanApplicationController {

    private final LoanApplicationService service;
    @PostMapping("/{borrowerId}/{lenderId}/{planId}")
    public ApiResponse<LoanApplicationResponseDto> applyLoan(
            @PathVariable Long borrowerId,
            @PathVariable Long lenderId,
            @PathVariable Long planId,
            @RequestBody LoanApplicationRequestDto requestDto) {

        return service.applyLoan(borrowerId, lenderId, planId, requestDto);
    }
    //VB
    @PatchMapping("/{applicationId}/decision")
    public ApiResponse<LoanApplicationResponseDto> updateLoanDecision(
            @PathVariable Long applicationId,
            @RequestParam Long lenderId,
            @RequestBody LoanApplicationApprovalRequestDto requestDto) {

        return service.updateLoanDecision(applicationId, lenderId, requestDto);
    }
    //vb
    @GetMapping("/{applicationId}")
    public ApiResponse<LoanApplicationResponseDto> getApplicationById(@PathVariable Long applicationId) {
        return service.getApplicationById(applicationId);
    }
    //VB
    @GetMapping("/lenderApplications/lenderId/{lenderId}")
    public ApiResponse<List<LoanApplicationResponseDto>> getApplicationsByLenderId(@PathVariable Long lenderId) {
        return service.getApplicationsByLenderId(lenderId);
    }
    //VB
    @GetMapping("/lenderApplications/borrowerId/{borrowerId}")
    public ApiResponse<List<LoanApplicationResponseDto>> getApplicationsByBorrowerId(@PathVariable Long borrowerId) {
        return service.getApplicationsByBorrowerId(borrowerId);
    }
//    @PostMapping
//    public ApiResponse<LoanApplicationResponseDto> create(@RequestBody LoanApplicationRequestDto dto) {
//        return service.createApplication(dto);
//    }
//
//    @GetMapping("/{id}")
//    public ApiResponse<LoanApplicationResponseDto> getById(@PathVariable Long id) {
//        return service.getById(id);
//    }
//
//    @GetMapping("/borrower/{borrowerId}")
//    public ApiResponse<List<LoanApplicationResponseDto>> getByBorrower(@PathVariable Long borrowerId) {
//        return service.getByBorrower(borrowerId);
//    }
//
//    @GetMapping("/lender/{lenderId}")
//    public ApiResponse<List<LoanApplicationResponseDto>> getByLender(@PathVariable Long lenderId) {
//        return service.getByLender(lenderId);
//    }
//
//    @PutMapping("/cancel/{id}")
//    public ApiResponse<String> cancel(@PathVariable Long id) {
//        return service.cancelApplication(id);
//    }
}