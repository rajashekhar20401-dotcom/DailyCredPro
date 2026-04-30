package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.constant.KycDocumentType;
import com.unqiuehire.kashflow.constant.KycOwnerType;
import com.unqiuehire.kashflow.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByOwnerTypeAndOwnerId(KycOwnerType ownerType, Long ownerId);

    Optional<KycDocument> findByOwnerTypeAndOwnerIdAndDocumentType(
            KycOwnerType ownerType,
            Long ownerId,
            KycDocumentType documentType
    );
}