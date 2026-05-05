package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class RewardEventResponseDto {
    private Long id;
    private Long loanId;
    private Long repaymentId;
    private Long borrowerId;
    private Long lenderId;
    private LocalDate eventDate;
    private String reasonType;
    private Double rewardPercent;
    private Double baseAmount;
    private Double rewardAmount;
    private String note;
    private LocalDateTime createdAt;
}