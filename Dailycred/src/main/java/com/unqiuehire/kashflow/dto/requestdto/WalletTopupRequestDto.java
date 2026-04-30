package com.unqiuehire.kashflow.dto.requestdto;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletTopupRequestDto {
    private WalletOwnerType walletOwnerType;
    private Long ownerId;
    private BigDecimal amount;
    private String description;
}
