package com.unqiuehire.kashflow.dto.requestdto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LoanRequestDto {

    private Long loanApplicationId;
    private Long borrowerId;
    private Long lenderId;
    private Long planId;

    private Double totalAmount;
    private Double sanctionedAmount;
    private Double interestPerDay;
    private Double penaltyAmount;
    private Integer tenureDays;
    private LocalDate startDate;

    private Boolean isCustomLoan;

    private Double customInterestPercent;

    private Integer customTenureDays;

}