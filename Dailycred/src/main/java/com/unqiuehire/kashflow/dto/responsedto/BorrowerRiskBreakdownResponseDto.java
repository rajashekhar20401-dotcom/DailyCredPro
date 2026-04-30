package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BorrowerRiskBreakdownResponseDto {
    private Long borrowerId;
    private String borrowerName;
    private Integer internalCreditScore;
    private Integer riskScore;
    private String riskCategory;
    private Integer totalLoansWithLender;
    private Double currentOutstanding;
    private Integer totalMissedDays;
    private Integer totalLatePayments;
}
