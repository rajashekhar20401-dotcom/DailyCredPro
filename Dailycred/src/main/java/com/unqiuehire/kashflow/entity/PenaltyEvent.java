package com.unqiuehire.kashflow.entity;

import com.unqiuehire.kashflow.constant.PenaltyReasonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "penalty_event")
@Getter
@Setter
public class PenaltyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId;

    private Long repaymentId;

    @Column(nullable = false)
    private Long borrowerId;

    @Column(nullable = false)
    private Long lenderId;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PenaltyReasonType reasonType;

    @Column(nullable = false)
    private Integer triggerCount;

    @Column(nullable = false)
    private Integer thresholdValue;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal dailyInterestAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Double penaltyPercent;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Boolean resolved = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.resolved == null) {
            this.resolved = false;
        }
    }
}