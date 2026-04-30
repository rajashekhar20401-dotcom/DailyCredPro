package com.unqiuehire.kashflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Getter
@Setter
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    private Long loanApplicationId;
    private Long borrowerId;
    private Long lenderId;
    private Long planId;

    private Double totalAmount;
    private Double sanctionedAmount;
    private Double interestPerDay;
    private Double penaltyAmount;
    private Double remainingAmount;
    private Integer tenureDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double dailyEmi;

    private Boolean isClosed;

    @Column(precision = 19, scale = 2)
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    private Integer missedDaysCount = 0;
    private Integer partialDaysCount = 0;
    private Integer advancePaidDaysCount = 0;

    private Integer consecutiveMissedDays = 0;
    private Integer consecutivePartialDays = 0;

    private LocalDate nextDueDate;
    private LocalDate lastPaymentDate;

    private Boolean closedEarly = false;
    private Boolean closedLate = false;

    @Column(precision = 19, scale = 2)
    private BigDecimal disbursedAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalRepayableAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal principalOutstanding;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalPenaltyAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal interestDeductionAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal interestDeductionPercent;

    private Boolean isCustomLoan;

    @Column(precision = 5, scale = 2)
    private BigDecimal customInterestPercent;

    @Column(precision = 5, scale = 2)
    private BigDecimal platformFeeRate = BigDecimal.valueOf(1.00);

    @Column(precision = 19, scale = 2)
    private BigDecimal platformFeeAmount = BigDecimal.ZERO;

    private Boolean platformFeeCharged = false;

    @Column(nullable = false)
    private Boolean platformFeeTransferred = false;
}