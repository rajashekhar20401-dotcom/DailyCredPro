package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.ApplicationStatus;
import com.unqiuehire.kashflow.constant.EmployeeType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoanApplicationResponseDto {

    private Long applicationId;
    private Long borrowerId;
    private Long lenderId;
    private Long planId;
    private Double loanAmount;
    private Integer age;
    private Double monthlyIncome;
    private EmployeeType employeeType;
    private String pinCode;
    private Boolean isEducated;
    private String certificates;
    private String collateral;
    private ApplicationStatus applicationStatus;
    private String remarks;
    private LocalDateTime appliedAt;
}