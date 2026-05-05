package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RiskAnalysisResultDto {

    private Integer riskScore;
    private String riskCategory;
    private String recommendation;
}
