package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.constant.KycOwnerType;
import com.unqiuehire.kashflow.constant.KycVerificationStatus;
import com.unqiuehire.kashflow.dto.responsedto.KycDocumentResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.KycSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.OcrResultDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.KycDocument;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.KycDocumentRepository;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.service.InternalCreditScoreService;
import com.unqiuehire.kashflow.service.KycService;
import com.unqiuehire.kashflow.service.OcrService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final BorrowerRepository borrowerRepository;
    private final LenderRepository lenderRepository;
    private final OcrService ocrService;
    private final InternalCreditScoreService internalCreditScoreService;

    @Override
    @Transactional
    public KycDocumentResponseDto uploadDocument(KycOwnerType ownerType, Long ownerId, KycDocumentType documentType, MultipartFile file) {
        validateOwnerExists(ownerType, ownerId);

        try {
            KycDocument document = kycDocumentRepository
                    .findByOwnerTypeAndOwnerIdAndDocumentType(ownerType, ownerId, documentType)
                    .orElseGet(KycDocument::new);

            document.setOwnerType(ownerType);
            document.setOwnerId(ownerId);
            document.setDocumentType(documentType);
            document.setFileName(file.getOriginalFilename());
            document.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            document.setFileData(file.getBytes());
            document.setVerificationStatus(KycVerificationStatus.UPLOADED);
            document.setManualReviewRequired(false);
            document.setVerificationRemarks("Document uploaded successfully");
            document.setUploadedAt(LocalDateTime.now());

            KycDocument saved = kycDocumentRepository.save(document);
            updateKycProgress(ownerType, ownerId);

            return map(saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload KYC document: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocumentResponseDto> getDocuments(KycOwnerType ownerType, Long ownerId) {        validateOwnerExists(ownerType, ownerId);
        return kycDocumentRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KycSummaryResponseDto getKycSummary(KycOwnerType ownerType, Long ownerId) {
        validateOwnerExists(ownerType, ownerId);

        List<KycDocument> docs = kycDocumentRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId);

        boolean aadhaar = hasDoc(docs, KycDocumentType.AADHAAR);
        boolean pan = hasDoc(docs, KycDocumentType.PAN);
        boolean selfie = hasDoc(docs, KycDocumentType.SELFIE);
        boolean signature = hasDoc(docs, KycDocumentType.SIGNATURE);

        int completed = 0;
        if (aadhaar) completed++;
        if (pan) completed++;
        if (selfie) completed++;
        if (signature) completed++;

        boolean manualReviewRequired = docs.stream().anyMatch(KycDocument::getManualReviewRequired);
        int completionPercent = completed * 25;

        KycSummaryResponseDto dto = new KycSummaryResponseDto();
        dto.setOwnerType(ownerType.name());
        dto.setOwnerId(ownerId);
        dto.setAadhaarUploaded(aadhaar);
        dto.setPanUploaded(pan);
        dto.setSelfieUploaded(selfie);
        dto.setSignatureUploaded(signature);
        dto.setCompletionPercent(completionPercent);
        dto.setManualReviewRequired(manualReviewRequired);
        dto.setOverallStatus(manualReviewRequired ? "MANUAL_REVIEW_REQUIRED" : (completionPercent == 100 ? "COMPLETE" : "INCOMPLETE"));
        return dto;
    }

    @Override
    @Transactional
    public OcrResultDto processOcr(Long documentId) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("KYC document not found"));

        OcrResultDto result = ocrService.extract(document.getFileData(), document.getDocumentType());

        document.setExtractedText(result.getExtractedText());
        document.setExtractedDocumentNumber(result.getExtractedDocumentNumber());
        document.setVerificationStatus(result.getVerificationStatus());
        document.setManualReviewRequired(result.getManualReviewRequired());
        document.setVerificationRemarks(result.getVerificationRemarks());
        document.setProcessedAt(LocalDateTime.now());

        compareWithProfile(document);

        KycDocument saved = kycDocumentRepository.save(document);
        updateKycProgress(saved.getOwnerType(), saved.getOwnerId());

        OcrResultDto dto = new OcrResultDto();
        dto.setDocumentId(saved.getDocumentId());
        dto.setExtractedText(saved.getExtractedText());
        dto.setExtractedDocumentNumber(saved.getExtractedDocumentNumber());
        dto.setVerificationStatus(saved.getVerificationStatus());
        dto.setManualReviewRequired(saved.getManualReviewRequired());
        dto.setVerificationRemarks(saved.getVerificationRemarks());
        return dto;
    }

    private void compareWithProfile(KycDocument document) {
        if (document.getDocumentType() == KycDocumentType.AADHAAR && document.getExtractedDocumentNumber() != null) {
            if (document.getOwnerType() == KycOwnerType.BORROWER) {
                Borrower borrower = borrowerRepository.findById(document.getOwnerId()).orElse(null);
                if (borrower != null && borrower.getAadharCardNumber() != null) {
                    if (document.getExtractedDocumentNumber().equals(borrower.getAadharCardNumber().replaceAll("\\s+", ""))) {
                        document.setVerificationStatus(KycVerificationStatus.VERIFIED);
                        document.setVerificationRemarks("Aadhaar OCR matched borrower profile data");
                    } else {
                        document.setVerificationStatus(KycVerificationStatus.MISMATCH);
                        document.setManualReviewRequired(true);
                        document.setVerificationRemarks("Aadhaar OCR did not match borrower profile data");
                    }
                }
            } else {
                Lender lender = lenderRepository.findById(document.getOwnerId()).orElse(null);
                if (lender != null && lender.getAadharCardNumber() != null) {
                    if (document.getExtractedDocumentNumber().equals(lender.getAadharCardNumber().replaceAll("\\s+", ""))) {
                        document.setVerificationStatus(KycVerificationStatus.VERIFIED);
                        document.setVerificationRemarks("Aadhaar OCR matched lender profile data");
                    } else {
                        document.setVerificationStatus(KycVerificationStatus.MISMATCH);
                        document.setManualReviewRequired(true);
                        document.setVerificationRemarks("Aadhaar OCR did not match lender profile data");
                    }
                }
            }
        }

        if (document.getDocumentType() == KycDocumentType.PAN && document.getExtractedDocumentNumber() != null) {
            if (document.getOwnerType() == KycOwnerType.BORROWER) {
                Borrower borrower = borrowerRepository.findById(document.getOwnerId()).orElse(null);
                if (borrower != null && borrower.getPanCardNumber() != null) {
                    if (document.getExtractedDocumentNumber().equalsIgnoreCase(borrower.getPanCardNumber())) {
                        document.setVerificationStatus(KycVerificationStatus.VERIFIED);
                        document.setVerificationRemarks("PAN OCR matched borrower profile data");
                    } else {
                        document.setVerificationStatus(KycVerificationStatus.MISMATCH);
                        document.setManualReviewRequired(true);
                        document.setVerificationRemarks("PAN OCR did not match borrower profile data");
                    }
                }
            } else {
                Lender lender = lenderRepository.findById(document.getOwnerId()).orElse(null);
                if (lender != null && lender.getPanCardNumber() != null) {
                    if (document.getExtractedDocumentNumber().equalsIgnoreCase(lender.getPanCardNumber())) {
                        document.setVerificationStatus(KycVerificationStatus.VERIFIED);
                        document.setVerificationRemarks("PAN OCR matched lender profile data");
                    } else {
                        document.setVerificationStatus(KycVerificationStatus.MISMATCH);
                        document.setManualReviewRequired(true);
                        document.setVerificationRemarks("PAN OCR did not match lender profile data");
                    }
                }
            }
        }
    }

    private void validateOwnerExists(KycOwnerType ownerType, Long ownerId) {
        if (ownerType == KycOwnerType.BORROWER) {
            borrowerRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("Borrower not found"));
        } else {
            lenderRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("Lender not found"));
        }
    }

    private boolean hasDoc(List<KycDocument> docs, KycDocumentType type) {
        return docs.stream().anyMatch(d -> d.getDocumentType() == type);
    }

    private void updateKycProgress(KycOwnerType ownerType, Long ownerId) {
        KycSummaryResponseDto summary = getKycSummary(ownerType, ownerId);

        if (ownerType == KycOwnerType.BORROWER) {
            Borrower borrower = borrowerRepository.findById(ownerId).orElse(null);
            if (borrower != null) {
                borrower.setKycCompletionPercent(summary.getCompletionPercent());
                borrower.setKycVerified("COMPLETE".equals(summary.getOverallStatus()) && !summary.getManualReviewRequired());

                int generatedScore = internalCreditScoreService.calculateInternalCreditScore(
                        borrower,
                        Collections.emptyList(),
                        Collections.emptyList()
                );
                borrower.setInternalCreditScore(generatedScore);
                borrower.setCibil(generatedScore);
                borrower.setEligibilityScore(generatedScore);

                borrowerRepository.save(borrower);
            }
        } else {
            Lender lender = lenderRepository.findById(ownerId).orElse(null);
            if (lender != null) {
                lender.setKycCompletionPercent(summary.getCompletionPercent());
                lender.setKycVerified("COMPLETE".equals(summary.getOverallStatus()) && !summary.getManualReviewRequired());
                lenderRepository.save(lender);
            }
        }
    }

    private KycDocumentResponseDto map(KycDocument document) {
        KycDocumentResponseDto dto = new KycDocumentResponseDto();
        dto.setDocumentId(document.getDocumentId());
        dto.setOwnerType(document.getOwnerType());
        dto.setOwnerId(document.getOwnerId());
        dto.setDocumentType(document.getDocumentType());
        dto.setFileName(document.getFileName());
        dto.setContentType(document.getContentType());
        dto.setExtractedText(document.getExtractedText());
        dto.setExtractedDocumentNumber(document.getExtractedDocumentNumber());
        dto.setVerificationStatus(document.getVerificationStatus());
        dto.setManualReviewRequired(document.getManualReviewRequired());
        dto.setVerificationRemarks(document.getVerificationRemarks());
        dto.setUploadedAt(document.getUploadedAt());
        dto.setProcessedAt(document.getProcessedAt());
        return dto;
    }
}