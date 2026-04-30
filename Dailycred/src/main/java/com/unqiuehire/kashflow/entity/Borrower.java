package com.unqiuehire.kashflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "borrower",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_borrower_phone_number", columnNames = "phone_number"),
                @UniqueConstraint(name = "uk_borrower_aadhar_card_number", columnNames = "aadhar_card_number"),
                @UniqueConstraint(name = "uk_borrower_pan_card_number", columnNames = "pan_card_number")
        }
)
@Getter
@Setter
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "borrower_id")
    private Long borrowerId;

    @Column(name = "borrower_name", nullable = false, length = 100)
    private String borrowerName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "cibil", nullable = false)
    private Integer cibil;

    @Column(name = "aadhar_card_number", unique = true, length = 20)
    private String aadharCardNumber;

    @Column(name = "pan_card_number", unique = true, length = 20)
    private String panCardNumber;

    // ---------- to check stability of profile using these fields ----------//

    @Column(name="monthly_income")
    private BigDecimal monthlyIncome;

    @Column(name="income_type",length=50)
    private String incomeType;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "years_in_current_work")
    private Integer yearsInCurrentWork;

    @Column(name = "dependents_count")
    private Integer dependentsCount;

    @Column(name = "house_owned")
    private Boolean houseOwned = false;

    @Column(name = "shop_owned")
    private Boolean shopOwned = false;

    @Column(name = "income_proof_uploaded")
    private Boolean incomeProofUploaded = false;

    @Column(name = "property_owned")
    private Boolean propertyOwned = false;

    @Column(name = "property_type", length = 100)
    private String propertyType;

    @Column(name = "collateral_provided")
    private Boolean collateralProvided = false;

    @Column(name = "collateral_type", length = 100)
    private String collateralType;

    @Column(name = "kyc_completion_percent")
    private Integer kycCompletionPercent = 0;

    @Column(name = "kyc_verified")
    private Boolean kycVerified = false;

    // ---------- admin / review ----------
    @Column(name = "fraud_flag")
    private Boolean fraudFlag = false;

    @Column(name = "manual_review_flag")
    private Boolean manualReviewFlag = false;

    @Column(name = "blacklisted")
    private Boolean blacklisted = false;

    @Column(name = "frozen")
    private Boolean frozen = false;

    @Column(name = "freeze_reason", length = 500)
    private String freezeReason;

    @Column(name = "fraud_reason", length = 500)
    private String fraudReason;

    // ---------- derived borrower analytics ----------
    @Column(name = "risk_score")
    private Integer riskScore = 50;

    @Column(name = "internal_credit_score")
    private Integer internalCreditScore = 0;

    @Column(name = "eligibility_score")
    private Integer eligibilityScore = 50;

    @Column(name = "risk_category", length = 50)
    private String riskCategory = "UNASSESSED";

    @Column(name = "eligibility_tier", length = 50)
    private String eligibilityTier = "UNASSESSED";

    @Column(name = "eligibility_status", length = 50)
    private String eligibilityStatus = "PENDING_REVIEW";

    @Column(name = "max_eligible_loan_amount", precision = 19, scale = 2)
    private BigDecimal maxEligibleLoanAmount = BigDecimal.ZERO;

    @Column(name = "current_outstanding_amount", precision = 19, scale = 2)
    private BigDecimal currentOutstandingAmount = BigDecimal.ZERO;

    @Column(name = "active_loan_count")
    private Integer activeLoanCount = 0;

    @Column(name = "total_loans_taken")
    private Integer totalLoansTaken = 0;

    @Column(name = "loans_closed_successfully")
    private Integer loansClosedSuccessfully = 0;

    @Column(name = "loans_closed_early")
    private Integer loansClosedEarly = 0;

    @Column(name = "total_missed_days")
    private Integer totalMissedDays = 0;

    @Column(name = "total_partial_days")
    private Integer totalPartialDays = 0;

    @Column(name = "total_advance_days")
    private Integer totalAdvanceDays = 0;

    @Column(name = "total_late_payments")
    private Integer totalLatePayments = 0;

    @Column(name = "max_consecutive_missed_days")
    private Integer maxConsecutiveMissedDays = 0;

    @Column(name = "defaulted_loan_count")
    private Integer defaultedLoanCount = 0;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "last_known_latitude")
    private Double lastKnownLatitude;

    @Column(name = "last_known_longitude")
    private Double lastKnownLongitude;

    @Column(name = "location_consent_given")
    private Boolean locationConsentGiven = false;

    @Column(name = "last_location_updated_at")
    private java.time.LocalDateTime lastLocationUpdatedAt;

    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "terms_accepted")
    private Boolean termsAccepted = false;

    @Column(name = "terms_accepted_at")
    private java.time.LocalDateTime termsAcceptedAt;

    @Column(name = "terms_version", length = 50)
    private String termsVersion;

    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<LoanApplication> loanApplications = new ArrayList<>();
}