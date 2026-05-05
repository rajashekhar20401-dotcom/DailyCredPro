package com.unqiuehire.kashflow.dto.requestdto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashCollectionConfirmRequestDto {
    private String confirmationToken;
    private String borrowerNote;
}