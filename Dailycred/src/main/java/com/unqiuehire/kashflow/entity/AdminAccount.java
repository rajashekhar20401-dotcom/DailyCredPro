package com.unqiuehire.kashflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_admin_email", columnNames = "email")
        })
@Getter
@Setter
public class AdminAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminId;

    @Column(nullable = false, length = 100)
    private String adminName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean superAdmin = false;

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "default_max_active_loans", nullable = false)
    private Integer defaultMaxActiveLoans = 3;

    @Column(name = "default_max_eligible_loan_amount", precision = 19, scale = 2, nullable = false)
    private java.math.BigDecimal defaultMaxEligibleLoanAmount = java.math.BigDecimal.valueOf(500000);

    @Column(name = "premium_max_eligible_loan_amount", precision = 19, scale = 2, nullable = false)
    private java.math.BigDecimal premiumMaxEligibleLoanAmount = java.math.BigDecimal.valueOf(500000);

    @Column(name = "standard_max_eligible_loan_amount", precision = 19, scale = 2, nullable = false)
    private java.math.BigDecimal standardMaxEligibleLoanAmount = java.math.BigDecimal.valueOf(300000);

    @Column(name = "basic_max_eligible_loan_amount", precision = 19, scale = 2, nullable = false)
    private java.math.BigDecimal basicMaxEligibleLoanAmount = java.math.BigDecimal.valueOf(100000);

    @Column(name = "low_limit_max_eligible_loan_amount", precision = 19, scale = 2, nullable = false)
    private java.math.BigDecimal lowLimitMaxEligibleLoanAmount = java.math.BigDecimal.valueOf(50000);

    @Column(name = "default_borrower_radius_km", nullable = false)
    private Double defaultBorrowerRadiusKm = 30.0;

    @Column(name = "defaulter_consecutive_missed_days", nullable = false)
    private Integer defaulterConsecutiveMissedDays = 10;

    @Column(name = "penalty_trigger_missed_days", nullable = false)
    private Integer penaltyTriggerMissedDays = 5;

    @Column(name = "penalty_trigger_partial_days", nullable = false)
    private Integer penaltyTriggerPartialDays = 10;

    @Column(name = "penalty_percent_of_daily_interest", nullable = false)
    private Double penaltyPercentOfDailyInterest = 1.0;

    @Column(name = "platform_fee_percent", nullable = false)
    private Double platformFeePercent = 1.0;

    @Column(name = "manual_review_risk_threshold", nullable = false)
    private Integer manualReviewRiskThreshold = 60;
}