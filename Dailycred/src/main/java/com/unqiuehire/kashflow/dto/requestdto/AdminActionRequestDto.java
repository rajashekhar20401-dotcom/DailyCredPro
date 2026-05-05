package com.unqiuehire.kashflow.dto.requestdto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminActionRequestDto {
    private String reason;

    private Integer defaultMaxActiveLoans;
    private java.math.BigDecimal defaultMaxEligibleLoanAmount;
    private java.math.BigDecimal premiumMaxEligibleLoanAmount;
    private java.math.BigDecimal standardMaxEligibleLoanAmount;
    private java.math.BigDecimal basicMaxEligibleLoanAmount;
    private java.math.BigDecimal lowLimitMaxEligibleLoanAmount;
    private Double defaultBorrowerRadiusKm;
    private Integer defaulterConsecutiveMissedDays;
    private Integer penaltyTriggerMissedDays;
    private Integer penaltyTriggerPartialDays;
    private Double penaltyPercentOfDailyInterest;
    private Double platformFeePercent;
    private Integer manualReviewRiskThreshold;

    private Integer overrideMaxActiveLoans;
    private java.math.BigDecimal overrideMaxEligibleLoanAmount;
    private String overrideEligibilityTier;
    private String overrideEligibilityStatus;
}