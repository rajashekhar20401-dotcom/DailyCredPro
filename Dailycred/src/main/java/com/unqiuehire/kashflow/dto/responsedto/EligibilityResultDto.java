package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EligibilityResultDto {

    private String eligibilityTier;
    private String eligibilityStatus;
    private Double maxEligibleLoanAmount;
    private Boolean collateralRequired;
    private String reason;
}
