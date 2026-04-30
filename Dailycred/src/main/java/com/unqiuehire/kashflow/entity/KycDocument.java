package com.unqiuehire.kashflow.entity;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.constant.KycOwnerType;
import com.unqiuehire.kashflow.constant.KycVerificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_document")
@Getter
@Setter
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycOwnerType ownerType;

    @Column(nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycDocumentType documentType;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] fileData;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column(length = 50)
    private String extractedDocumentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycVerificationStatus verificationStatus = KycVerificationStatus.UPLOADED;

    @Column(nullable = false)
    private Boolean manualReviewRequired = false;

    @Column(length = 500)
    private String verificationRemarks;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    private LocalDateTime processedAt;
}