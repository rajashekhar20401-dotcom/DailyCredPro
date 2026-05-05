package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private Integer internalCreditScore;
    private Integer riskScore;
    private Integer eligibilityScore;
    private String riskCategory;
    private String eligibilityTier;
    private String eligibilityStatus;
    private BigDecimal maxEligibleLoanAmount;
    private BigDecimal currentOutstandingAmount;
    private Integer activeLoanCount;
    private Integer totalLoansTaken;
    private Integer loansClosedSuccessfully;
    private Integer loansClosedEarly;
    private Integer totalMissedDays;
    private Integer totalPartialDays;
    private Integer totalAdvanceDays;
    private Integer totalLatePayments;
    private Integer maxConsecutiveMissedDays;
    private Integer defaultedLoanCount;
    private Boolean fraudFlag;
    private Boolean manualReviewFlag;
    private Boolean blacklisted;

    private String email;
    private Boolean notificationEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean termsAccepted;
    private LocalDateTime termsAcceptedAt;
    private String termsVersion;
}