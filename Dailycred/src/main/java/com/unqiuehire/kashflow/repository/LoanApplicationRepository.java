package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.constant.ApplicationStatus;
import com.unqiuehire.kashflow.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByBorrower_BorrowerId(Long borrowerId);

    List<LoanApplication> findByLender_LenderId(Long lenderId);

    List<LoanApplication> findByLoanPlan_Lender_LenderId(Long lenderId);

    boolean existsByBorrower_BorrowerIdAndLoanPlan_IdAndStatus(Long borrowerId, Long planId, ApplicationStatus status);
}