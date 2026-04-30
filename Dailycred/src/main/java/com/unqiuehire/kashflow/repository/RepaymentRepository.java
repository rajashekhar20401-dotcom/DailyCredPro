package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    List<Repayment> findByLoanLoanId(Long loanId);

    List<Repayment> findByLoanApplicationApplicationId(Long loanApplicationId);

    List<Repayment> findByBorrowerIdOrderByPaymentDateDesc(Long borrowerId);
}