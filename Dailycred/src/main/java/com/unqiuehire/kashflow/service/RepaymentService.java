package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.RepaymentRequestDTO;
import com.unqiuehire.kashflow.dto.responsedto.RepaymentResponseDTO;

import java.util.List;

public interface RepaymentService {

    RepaymentResponseDTO makePayment(RepaymentRequestDTO request);

    List<RepaymentResponseDTO> getByLoan(Long loanId);

    List<RepaymentResponseDTO> getByLoanApplication(Long loanApplicationId);

    List<RepaymentResponseDTO> getByBorrower(Long borrowerId);
}