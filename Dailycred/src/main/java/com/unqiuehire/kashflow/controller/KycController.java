package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.constant.KycOwnerType;
import com.unqiuehire.kashflow.dto.responsedto.KycDocumentResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.KycSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.OcrResultDto;
import com.unqiuehire.kashflow.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KycController {

    private final KycService kycService;

    @PostMapping("/{ownerType}/{ownerId}/{documentType}/upload")
    public KycDocumentResponseDto uploadDocument(@PathVariable KycOwnerType ownerType,
                                                 @PathVariable Long ownerId,
                                                 @PathVariable KycDocumentType documentType,
                                                 @RequestParam("file") MultipartFile file) {
        return kycService.uploadDocument(ownerType, ownerId, documentType, file);
    }

    @GetMapping("/{ownerType}/{ownerId}/documents")
    public List<KycDocumentResponseDto> getDocuments(@PathVariable KycOwnerType ownerType,
                                                     @PathVariable Long ownerId) {
        return kycService.getDocuments(ownerType, ownerId);
    }

    @GetMapping("/{ownerType}/{ownerId}/summary")
    public KycSummaryResponseDto getKycSummary(@PathVariable KycOwnerType ownerType,
                                               @PathVariable Long ownerId) {
        return kycService.getKycSummary(ownerType, ownerId);
    }

    @PostMapping("/documents/{documentId}/ocr")
    public OcrResultDto processOcr(@PathVariable Long documentId) {
        return kycService.processOcr(documentId);
    }
}