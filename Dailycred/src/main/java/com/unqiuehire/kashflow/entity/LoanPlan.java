package com.unqiuehire.kashflow.entity;

import com.unqiuehire.kashflow.constant.EmployeeType;
import com.unqiuehire.kashflow.constant.LoanPlanStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "loan_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String planName;
    private double amount;
    private double interestPerDay;
    private double penaltyAmount;
    private int planDuration;
    private double maxRadius;
    private int minCibil;
    private Integer minAge;
    private Integer MaxAge;
    private double minMonthlyIncome;
    private String servicePinCode;
    private Integer maxActiveLoans;

    @Enumerated(EnumType.STRING)
    private EmployeeType employeeType;

    @Enumerated(EnumType.STRING)
    private LoanPlanStatus status;

    //  RELATIONSHIP
    @ManyToOne
    @JoinColumn(name = "lender_id", nullable = false)
    private Lender lender;

//    @OneToMany(mappedBy = "loanPlan", cascade = CascadeType.ALL)
//    private List<LoanApplication> applications;
//
////    @Id
////    @GeneratedValue(strategy = GenerationType.IDENTITY)
////    private Long id;
////
////    private String planName;
////    private Long lenderId;
////    private double amount;
////    private double interestPerDay;
////    private double penaltyAmount;
////    private int planDuration;
////    private double maxRadius;
////    private int minCibil;
////
////    @Enumerated(EnumType.STRING)
////    private LoanPlanStatus status;
}