package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycSummaryResponseDto {
    private String ownerType;
    private Long ownerId;

    private Boolean aadhaarUploaded;
    private Boolean panUploaded;
    private Boolean selfieUploaded;
    private Boolean signatureUploaded;

    private Integer completionPercent;
    private Boolean manualReviewRequired;
    private String overallStatus;
}