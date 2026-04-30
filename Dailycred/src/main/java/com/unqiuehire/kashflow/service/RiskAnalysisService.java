package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.responsedto.RiskAnalysisResultDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.Repayment;

import java.util.List;

public interface RiskAnalysisService {

    RiskAnalysisResultDto analyze(Borrower borrower, List<Loan> loans, List<Repayment> repayments);
}

