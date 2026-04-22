package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BorrowerResponseDto {

    private Long borrowerId;
    private String borrowerName;
    private String dateOfBirth;
    private Boolean isActive;
    private String phoneNumber;
    private String pincode;
    private String address;
    private Integer cibil;
    private String aadharCardNumber;
    private String panCardNumber;

}