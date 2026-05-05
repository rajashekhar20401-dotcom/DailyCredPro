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
    private Integer defaultedLoans;

    private Double totalPrincipalDisbursed;
    private Double expectedTotalRepayment;
    private Double totalCollected;
    private Double currentOutstanding;
    private Double totalOutstanding; // backward compatibility
    private Double totalOverdue;
    private Double currentOutstandingExposure; // backward compatibility
    private Double recoverableAmount; // backward compatibility

    private Double totalPenaltyAccrued;
    private Double totalPenaltyCollected; // backward compatibility

    private Double estimatedGrossProfit;
    private Double projectedProfit; // backward compatibility
    private Double estimatedPlatformFee;
    private Double estimatedNetProfit;
    private Double capitalReadyForReuse; // backward compatibility

    private Integer paidTodayCount;
    private Integer paidToday; // backward compatibility
    private Integer missedTodayCount;
    private Integer missedToday; // backward compatibility
    private Integer partialTodayCount;
    private Integer partialToday; // backward compatibility
    private Integer advanceTodayCount;
    private Integer advanceToday; // backward compatibility

    private Integer currentlyOverdueBorrowerCount;
    private Integer overdueBorrowers; // backward compatibility

    private Double projectedCollectionRate;
    private Double projectedRecoveryRate;
    private Double averageExpectedReturnPerLoan;
}