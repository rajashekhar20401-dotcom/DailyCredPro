package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.entity.LoanPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanPlanRepository extends JpaRepository<LoanPlan, Long> {
    List<LoanPlan> findByLender_LenderId(Long lenderId);
}