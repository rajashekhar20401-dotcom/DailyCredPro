package com.unqiuehire.kashflow.entity;

import com.unqiuehire.kashflow.constant.RewardReasonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_event")
@Getter
@Setter
public class RewardEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId;

    private Long repaymentId;

    @Column(nullable = false)
    private Long borrowerId;

    @Column(nullable = false)
    private Long lenderId;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RewardReasonType reasonType;

    @Column(nullable = false)
    private Double rewardPercent;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal rewardAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
