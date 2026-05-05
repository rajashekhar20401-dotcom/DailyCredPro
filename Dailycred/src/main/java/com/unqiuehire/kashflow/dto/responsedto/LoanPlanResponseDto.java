package com.unqiuehire.kashflow.dto.responsedto;
import com.unqiuehire.kashflow.constant.EmployeeType;
import com.unqiuehire.kashflow.constant.LoanPlanStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanPlanResponseDto {
    private Long id;
    private String planName;
    private Long lenderId;
    private double amount;
    private double interestPerDay;
    private double penaltyAmount;
    private int planDuration;
//    private double maxRadius;
    private int minCibil;

    private Integer minAge;
    private Integer maxAge;
    private Double minMonthlyIncome;
    private String servicePinCode;
    private Integer maxActiveLoans;
    private EmployeeType employeeType;

    private LoanPlanStatus status;
}