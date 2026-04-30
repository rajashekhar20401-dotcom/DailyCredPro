package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LoanResponseDto {

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

    private Double disbursedAmount;
    private Double totalRepayableAmount;
    private Double overdueAmount;
    private Double totalPaidAmount;
    private Integer missedDaysCount;
    private Integer partialDaysCount;
    private Integer advancePaidDaysCount;
    private LocalDate nextDueDate;

    private Double platformFeeRate;
    private Double platformFeeAmount;
    private Boolean platformFeeCharged;
}