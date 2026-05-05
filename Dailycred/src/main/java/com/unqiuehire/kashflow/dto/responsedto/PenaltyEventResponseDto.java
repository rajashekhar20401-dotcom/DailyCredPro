package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PenaltyEventResponseDto {
    private Long id;
    private Long loanId;
    private Long repaymentId;
    private Long borrowerId;
    private Long lenderId;
    private LocalDate eventDate;
    private String reasonType;
    private Integer triggerCount;
    private Integer thresholdValue;
    private Double dailyInterestAmount;
    private Double penaltyPercent;
    private Double amount;
    private String note;
    private Boolean resolved;
    private LocalDateTime createdAt;
}