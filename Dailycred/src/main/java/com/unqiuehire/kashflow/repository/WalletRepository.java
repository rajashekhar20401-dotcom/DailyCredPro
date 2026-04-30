package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import com.unqiuehire.kashflow.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByWalletOwnerTypeAndOwnerId(WalletOwnerType walletOwnerType, Long ownerId);
}