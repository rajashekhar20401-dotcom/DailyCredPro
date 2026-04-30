package com.unqiuehire.kashflow.entity;

import com.unqiuehire.kashflow.constant.PaymentMode;
import com.unqiuehire.kashflow.constant.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "repayment")
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long borrowerId;

    private Double amountPaid;

    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String transactionReference;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    private Boolean isPartialPayment;
    private Boolean isEarlyPayment;
    private Boolean isMissedPayment;
    private Boolean isAdvancePayment;
    private Boolean isLatePayment;
    private Boolean isPreClosure;

    private Double interestAdded;
    private Double penaltyAmount;
    private Integer missedDays;
    private Double balanceAmount;

    private Double allocatedToOverdue;
    private Double allocatedToTodayDue;
    private Double allocatedToAdvance;
    private Double allocatedToPenalty;

    private Integer daysCovered;
}