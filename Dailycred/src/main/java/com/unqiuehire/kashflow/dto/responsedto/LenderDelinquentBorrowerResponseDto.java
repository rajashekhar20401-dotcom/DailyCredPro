package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LenderDelinquentBorrowerResponseDto {

    private Long borrowerId;
    private String borrowerName;
    private String phoneNumber;

    private Integer activeLoanCount;
    private Double totalOutstandingAmount;
    private Double totalOverdueAmount;

    private Integer totalMissedDays;
    private Integer maxConsecutiveMissedDays;

    private String riskCategory;
    private Integer riskScore;
}