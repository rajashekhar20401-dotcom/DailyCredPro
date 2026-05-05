package com.unqiuehire.kashflow.repository;
import com.unqiuehire.kashflow.entity.Lender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LenderRepository extends JpaRepository<Lender, Long> {
    boolean existsByAadharCardNumber(String aadharCardNumber);

    boolean existsByPanCardNumber(String panCardNumber);

    Optional<Lender> findByPhoneNumber(String phoneNumber);

    Optional<Lender> findByEmail(String email);
}