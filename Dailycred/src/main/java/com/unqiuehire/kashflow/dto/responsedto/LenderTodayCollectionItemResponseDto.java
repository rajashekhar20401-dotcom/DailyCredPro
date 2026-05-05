package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.PaymentMode;
import com.unqiuehire.kashflow.constant.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LenderTodayCollectionItemResponseDto {
    private Long borrowerId;
    private String borrowerName;
    private Long loanId;
    private Double amountPaid;
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private LocalDate paymentDate;
    private Double balanceAmount;
}
