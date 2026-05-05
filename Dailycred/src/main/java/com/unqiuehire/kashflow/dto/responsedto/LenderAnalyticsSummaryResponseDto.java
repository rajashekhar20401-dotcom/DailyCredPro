package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LenderAnalyticsSummaryResponseDto {

    private Long lenderId;
    private String lenderName;

    private Integer totalLoans;
    private Integer activeLoans;
    private Integer closedLoans;
    private Integer defaultedLoans;

    private Double totalPrincipalDisbursed;
    private Double expectedTotalRepayment;
    private Double totalCollected;
    private Double currentOutstanding;
    private Double totalOverdue;
    private Double totalPenaltyAccrued;

    private Double estimatedGrossProfit;
    private Double estimatedPlatformFee;
    private Double estimatedNetProfit;

    private Integer paidTodayCount;
    private Integer missedTodayCount;
    private Integer partialTodayCount;
    private Integer advanceTodayCount;
    private Integer currentlyOverdueBorrowerCount;

    private Double projectedCollectionRate;
    private Double projectedRecoveryRate;
    private Double averageExpectedReturnPerLoan;

    private List<LenderDelinquentBorrowerResponseDto> delinquentBorrowers;
}