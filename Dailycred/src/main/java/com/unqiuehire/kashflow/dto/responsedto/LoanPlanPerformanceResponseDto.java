package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanPlanPerformanceResponseDto {
    private Long planId;
    private String planName;
    private Integer totalLoans;
    private Integer activeLoans;
    private Integer closedLoans;
    private Double totalDisbursed;
    private Double totalCollected;
    private Double totalOutstanding;
    private Double projectedProfit;
}