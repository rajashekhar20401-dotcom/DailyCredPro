package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.entity.CashCollectionConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashCollectionConfirmationRepository extends JpaRepository<CashCollectionConfirmation, Long> {
    List<CashCollectionConfirmation> findByLenderIdOrderByCreatedAtDesc(Long lenderId);
    List<CashCollectionConfirmation> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);
}