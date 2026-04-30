package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.constant.KycOwnerType;
import com.unqiuehire.kashflow.dto.responsedto.KycDocumentResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.KycSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.OcrResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KycService {

    KycDocumentResponseDto uploadDocument(
            KycOwnerType ownerType,
            Long ownerId,
            KycDocumentType documentType,
            MultipartFile file
    );

    List<KycDocumentResponseDto> getDocuments(KycOwnerType ownerType, Long ownerId);

    KycSummaryResponseDto getKycSummary(KycOwnerType ownerType, Long ownerId);

    OcrResultDto processOcr(Long documentId);
}