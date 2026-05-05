package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.entity.RewardEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardEventRepository extends JpaRepository<RewardEvent, Long> {
    List<RewardEvent> findByLoanIdOrderByEventDateDescCreatedAtDesc(Long loanId);
}
