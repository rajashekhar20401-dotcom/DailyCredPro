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
    private Double amountPaid;
    private LocalDate paymentDate;

    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;

    private Boolean isPartialPayment;
    private Boolean isEarlyPayment;
    private Boolean isMissedPayment;

    private Double interestAdded;
    private Double penaltyAmount;
    private Integer missedDays;

    private Double balanceAmount;
    private Long borrowerId;

    private String transactionReference;
}