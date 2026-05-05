package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.dto.responsedto.OcrResultDto;

public interface OcrService {
    OcrResultDto extract(byte[] fileData, KycDocumentType documentType);
}