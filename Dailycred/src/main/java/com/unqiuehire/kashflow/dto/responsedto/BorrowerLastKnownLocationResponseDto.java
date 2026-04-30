package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BorrowerLastKnownLocationResponseDto {
    private Long borrowerId;
    private String borrowerName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime lastUpdatedAt;
    private String accessReason;
}