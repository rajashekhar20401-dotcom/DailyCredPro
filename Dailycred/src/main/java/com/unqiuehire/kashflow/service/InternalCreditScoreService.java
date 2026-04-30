package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.Repayment;

import java.util.List;

public interface InternalCreditScoreService {

    Integer calculateInternalCreditScore(Borrower borrower, List<Loan> loans, List<Repayment> repayments);
}
