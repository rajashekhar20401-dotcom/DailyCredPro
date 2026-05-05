package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.entity.PenaltyEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyEventRepository extends JpaRepository<PenaltyEvent, Long> {
    List<PenaltyEvent> findByLoanIdOrderByEventDateDescCreatedAtDesc(Long loanId);
}