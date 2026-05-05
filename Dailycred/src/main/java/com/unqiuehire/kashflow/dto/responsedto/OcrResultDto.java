package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.KycVerificationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OcrResultDto {
    private Long documentId;
    private String extractedText;
    private String extractedDocumentNumber;
    private KycVerificationStatus verificationStatus;
    private Boolean manualReviewRequired;
    private String verificationRemarks;
}