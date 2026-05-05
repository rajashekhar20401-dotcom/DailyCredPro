package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BorrowerRiskBreakdownResponseDto {
    private Long borrowerId;
    private String borrowerName;
    private String phoneNumber;

    private Integer internalCreditScore;
    private Integer riskScore;
    private String riskCategory;

    private Integer totalLoansWithLender;
    private Integer activeLoanCount;

    private Double currentOutstanding;
    private Double totalOverdueAmount;

    private Integer totalMissedDays;
    private Integer maxConsecutiveMissedDays;
    private Integer totalLatePayments;
}