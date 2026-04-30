package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LenderDashboardSummaryResponseDto {
    private Long lenderId;
    private Integer totalLoans;
    private Integer activeLoans;
    private Integer closedLoans;
    private Double totalPrincipalDisbursed;
    private Double totalCollected;
    private Double totalOutstanding;
    private Double totalPenaltyCollected;
    private Double projectedProfit;
    private Double currentOutstandingExposure;
    private Double recoverableAmount;
    private Double capitalReadyForReuse;
    private Integer paidToday;
    private Integer missedToday;
    private Integer partialToday;
    private Integer advanceToday;
    private Integer overdueBorrowers;
}
