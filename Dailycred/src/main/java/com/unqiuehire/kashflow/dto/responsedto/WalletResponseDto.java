package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.WalletOwnerType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletResponseDto {
    private Long walletId;
    private WalletOwnerType walletOwnerType;
    private Long ownerId;
    private BigDecimal balance;
    private Boolean active;
    private Boolean frozen;
}
