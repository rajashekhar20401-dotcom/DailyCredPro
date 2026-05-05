package com.unqiuehire.kashflow.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "lender")
@Getter
@Setter
public class Lender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lender_id")
    private Long lenderId;

    @Column(name = "lender_name", nullable = false)
    private String lenderName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "pincode", nullable = false)
    private String pincode;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "kyc_completion_percent")
    private Integer kycCompletionPercent = 0;

    @Column(name = "kyc_verified")
    private Boolean kycVerified = false;

    @Column(name = "frozen")
    private Boolean frozen = false;

    @Column(name = "freeze_reason", length = 500)
    private String freezeReason;

    @Column(name = "fraud_flag")
    private Boolean fraudFlag = false;

    @Column(name = "manual_review_flag")
    private Boolean manualReviewFlag = false;

    @Column(name = "blacklisted")
    private Boolean blacklisted = false;

    @Column(name = "fraud_reason", length = 500)
    private String fraudReason;

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

    @OneToMany(mappedBy = "lender")
    private List<LoanApplication> loanApplications;

    //  ONE TO MANY
    @OneToMany(mappedBy = "lender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanPlan> loanPlans;

    @Column(name = "aadhar_card_number",unique = true)
    private String aadharCardNumber;

    @Column(name = "pan_card_number",unique = true)
    private String panCardNumber;
}