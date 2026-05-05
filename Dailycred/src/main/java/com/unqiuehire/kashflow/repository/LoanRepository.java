package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByBorrowerId(Long borrowerId);
    List<Loan> findByLenderId(Long lenderId);
    boolean existsByLoanApplicationId(Long loanApplicationId);
}