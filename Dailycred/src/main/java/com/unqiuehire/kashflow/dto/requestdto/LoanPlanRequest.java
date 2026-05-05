package com.unqiuehire.kashflow.dto.requestdto;

import com.unqiuehire.kashflow.constant.EmployeeType;
import lombok.Data;

@Data
public class LoanPlanRequest {

    private String planName;
    private Long lenderId;
    private double amount;
    private double interestPerDay;
    private double penaltyAmount;
    private int planDuration;
    private int minCibil;

    private Integer minAge;
    private Integer maxAge;
    private double minMonthlyIncome;
    private String servicePinCode;
    private Integer maxActiveLoans;
    private EmployeeType employeeType;

    private String status;
}