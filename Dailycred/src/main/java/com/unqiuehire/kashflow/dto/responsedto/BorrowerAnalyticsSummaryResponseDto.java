package com.unqiuehire.kashflow.dto.responsedto;

import lombok.*;

@Getter
@Setter
public class BorrowerAnalyticsSummaryResponseDto {

    private Long borrowerId;
    private String borrowerName;

    private Integer currentCibil;
    private Integer internalCreditScore;
    private Integer riskScore;
    private String riskCategory;

    private String eligibilityTier;
    private String eligibilityStatus;
    private Double maxEligibleLoanAmount;
    private Boolean collateralRequired;
    private String recommendation;

    private Integer totalLoansTaken;
    private Integer activeLoans;
    private Integer closedLoans;
    private Integer defaultedLoanCount;

    private Integer totalRepaymentEvents;
    private Integer totalMissedDays;
    private Integer totalPartialPayments;
    private Integer totalAdvancePayments;
    private Integer totalLatePayments;
    private Integer totalPreClosures;
    private Integer maxConsecutiveMissedDays;

    private Double averageMissedDaysPerLoan;
    private Double totalDisbursedAmount;
    private Double totalRepaidAmount;
    private Double currentOutstandingAmount;
}
