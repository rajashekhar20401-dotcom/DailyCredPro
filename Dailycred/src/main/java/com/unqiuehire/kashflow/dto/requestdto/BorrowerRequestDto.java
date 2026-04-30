package com.unqiuehire.kashflow.dto.requestdto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerRequestDto {

    private String borrowerName;
    private String dateOfBirth;
    private String password;
    private Boolean isActive;
    private String phoneNumber;
    private String pincode;
    private String address;
    private Integer cibil;
    private String aadharCardNumber;
    private String panCardNumber;

    private BigDecimal monthlyIncome;
    private String incomeType;
    private String employmentType;
    private Integer yearsInCurrentWork;
    private Integer dependentsCount;
    private Boolean houseOwned;
    private Boolean shopOwned;

    private Boolean incomeProofUploaded;
    private Boolean propertyOwned;
    private String propertyType;

    private Boolean collateralProvided;
    private String collateralType;

    private Integer kycCompletionPercent;
    private Boolean kycVerified;

    private String email;
    private Boolean notificationEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean termsAccepted;
    private String termsVersion;
}