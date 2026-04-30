package com.unqiuehire.kashflow.entity;

import com.unqiuehire.kashflow.constant.CashCollectionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_collection_confirmation")
@Getter
@Setter
public class CashCollectionConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long confirmationId;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Long loanApplicationId;

    @Column(nullable = false)
    private Long lenderId;

    @Column(nullable = false)
    private Long borrowerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, length = 10)
    private String generatedToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashCollectionStatus status;

    @Column(length = 500)
    private String lenderNote;

    @Column(length = 500)
    private String borrowerNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;
    private LocalDateTime rejectedAt;

    private Long repaymentId;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = CashCollectionStatus.PENDING_BORROWER_CONFIRMATION;
        }
    }
}
