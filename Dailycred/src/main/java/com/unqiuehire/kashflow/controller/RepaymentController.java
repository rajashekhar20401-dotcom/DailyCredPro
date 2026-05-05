package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.RepaymentRequestDTO;
import com.unqiuehire.kashflow.dto.responsedto.RepaymentResponseDTO;
import com.unqiuehire.kashflow.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repayments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RepaymentController {

    private final RepaymentService repaymentService;

    @PostMapping
    public RepaymentResponseDTO makePayment(@RequestBody RepaymentRequestDTO requestDTO) {
        return repaymentService.makePayment(requestDTO);
    }

    @GetMapping("/loan/{loanId}")
    public List<RepaymentResponseDTO> getByLoan(@PathVariable Long loanId) {
        return repaymentService.getByLoan(loanId);
    }

    @GetMapping("/loan-application/{loanApplicationId}")
    public List<RepaymentResponseDTO> getByLoanApplication(@PathVariable Long loanApplicationId) {
        return repaymentService.getByLoanApplication(loanApplicationId);
    }

    @GetMapping("/borrower/{borrowerId}")
    public List<RepaymentResponseDTO> getByBorrower(@PathVariable Long borrowerId) {
        return repaymentService.getByBorrower(borrowerId);
    }
}