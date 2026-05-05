package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.constant.KycVerificationStatus;
import com.unqiuehire.kashflow.dto.responsedto.OcrResultDto;
import com.unqiuehire.kashflow.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OcrServiceImpl implements OcrService {

    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b");
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");

    @Value("${ocr.tesseract.enabled:false}")
    private boolean ocrEnabled;

    @Value("${ocr.tesseract.datapath:}")
    private String tessDataPath;

    @Value("${ocr.tesseract.language:eng}")
    private String language;

    @Value("${ocr.tesseract.psm:6}")
    private int pageSegMode;

    @Override
    public OcrResultDto extract(byte[] fileData, KycDocumentType documentType) {
        OcrResultDto result = new OcrResultDto();

        // OCR not available -> graceful fallback
        if (!ocrEnabled) {
            result.setExtractedText(null);
            result.setExtractedDocumentNumber(null);
            result.setVerificationStatus(KycVerificationStatus.MANUAL_REVIEW_REQUIRED);
            result.setManualReviewRequired(true);
            result.setVerificationRemarks("OCR is disabled in configuration. Manual review required.");
            return result;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileData));

            if (image == null) {
                result.setExtractedText(null);
                result.setExtractedDocumentNumber(null);
                result.setVerificationStatus(KycVerificationStatus.FAILED);
                result.setManualReviewRequired(true);
                result.setVerificationRemarks("Uploaded file could not be read as an image.");
                return result;
            }

            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage(language);
            tesseract.setPageSegMode(pageSegMode);

            String extractedText = tesseract.doOCR(image);
            String extractedNumber = null;
            KycVerificationStatus status = KycVerificationStatus.OCR_EXTRACTED;
            boolean manualReview = false;
            String remarks = "OCR processed successfully";

            if (documentType == KycDocumentType.AADHAAR) {
                Matcher matcher = AADHAAR_PATTERN.matcher(extractedText);
                if (matcher.find()) {
                    extractedNumber = matcher.group().replaceAll("\\s+", "");
                } else {
                    status = KycVerificationStatus.MANUAL_REVIEW_REQUIRED;
                    manualReview = true;
                    remarks = "Aadhaar number pattern not found. Manual review required.";
                }
            } else if (documentType == KycDocumentType.PAN) {
                Matcher matcher = PAN_PATTERN.matcher(extractedText.toUpperCase());
                if (matcher.find()) {
                    extractedNumber = matcher.group();
                } else {
                    status = KycVerificationStatus.MANUAL_REVIEW_REQUIRED;
                    manualReview = true;
                    remarks = "PAN pattern not found. Manual review required.";
                }
            } else {
                status = KycVerificationStatus.UPLOADED;
                remarks = "OCR is not used for this document type. Stored for manual verification.";
            }

            result.setExtractedText(extractedText);
            result.setExtractedDocumentNumber(extractedNumber);
            result.setVerificationStatus(status);
            result.setManualReviewRequired(manualReview);
            result.setVerificationRemarks(remarks);
            return result;

        } catch (TesseractException e) {
            log.error("Tesseract OCR failed", e);

            result.setExtractedText(null);
            result.setExtractedDocumentNumber(null);
            result.setVerificationStatus(KycVerificationStatus.MANUAL_REVIEW_REQUIRED);
            result.setManualReviewRequired(true);
            result.setVerificationRemarks("OCR engine failed: " + e.getMessage());
            return result;

        } catch (Exception e) {
            log.error("OCR processing failed", e);

            result.setExtractedText(null);
            result.setExtractedDocumentNumber(null);
            result.setVerificationStatus(KycVerificationStatus.FAILED);
            result.setManualReviewRequired(true);
            result.setVerificationRemarks("OCR processing failed: " + e.getMessage());
            return result;
        }
    }
}