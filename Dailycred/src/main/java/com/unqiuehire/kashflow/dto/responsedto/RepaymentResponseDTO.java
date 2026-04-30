package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.PaymentMode;
import com.unqiuehire.kashflow.constant.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RepaymentResponseDTO {

    private Long id;
    private Long borrowerId;

    private Double amountPaid;
    private LocalDate paymentDate;

    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;

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

    private String transactionReference;
}