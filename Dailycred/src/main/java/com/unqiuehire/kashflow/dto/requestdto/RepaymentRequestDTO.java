package com.unqiuehire.kashflow.dto.requestdto;

import com.unqiuehire.kashflow.constant.PaymentMode;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RepaymentRequestDTO {

    private Long loanId;
    private Long loanApplicationId;

    private Double amountPaid;
    private PaymentMode paymentMode;

    // optional, if null backend will use LocalDate.now()
    private LocalDate paymentDate;
}