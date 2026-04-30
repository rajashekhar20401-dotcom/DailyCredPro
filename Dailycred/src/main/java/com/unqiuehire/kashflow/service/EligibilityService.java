package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.responsedto.EligibilityResultDto;
import com.unqiuehire.kashflow.dto.responsedto.RiskAnalysisResultDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;

import java.util.List;

public interface EligibilityService {

    EligibilityResultDto evaluate(Borrower borrower, Integer internalCreditScore, RiskAnalysisResultDto riskResult, List<Loan> loans);
}
