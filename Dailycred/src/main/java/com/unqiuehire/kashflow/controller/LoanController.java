package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.LoanRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanResponseDto;
import com.unqiuehire.kashflow.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping
    public ApiResponse<LoanResponseDto> createLoan(@RequestBody LoanRequestDto dto) {
        return loanService.createLoan(dto);
    }

    @GetMapping("/{id}")
    public ApiResponse<LoanResponseDto> getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id);
    }

    @GetMapping("/borrower/{borrowerId}")
    public ApiResponse<List<LoanResponseDto>> getByBorrower(@PathVariable Long borrowerId) {
        return loanService.getLoansByBorrower(borrowerId);
    }

    @GetMapping("/lender/{lenderId}")
    public ApiResponse<List<LoanResponseDto>> getByLender(@PathVariable Long lenderId) {
        return loanService.getLoansByLender(lenderId);
    }

    @PutMapping("/close/{loanId}")
    public ApiResponse<String> closeLoan(@PathVariable Long loanId) {
        return loanService.closeLoan(loanId);
    }
}