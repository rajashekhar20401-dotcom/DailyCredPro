package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.constant.KycOwnerType;
import com.unqiuehire.kashflow.constant.KycVerificationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KycDocumentResponseDto {
    private Long documentId;
    private KycOwnerType ownerType;
    private Long ownerId;
    private KycDocumentType documentType;
    private String fileName;
    private String contentType;
    private String extractedText;
    private String extractedDocumentNumber;
    private KycVerificationStatus verificationStatus;
    private Boolean manualReviewRequired;
    private String verificationRemarks;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
}