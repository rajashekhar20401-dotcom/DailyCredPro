package com.unqiuehire.kashflow.dto.requestdto;

import com.unqiuehire.kashflow.constant.EmployeeType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanApplicationRequestDto {
//    private Long borrowerId;
//    private Long lenderId;
//    private Long planId;
    private Double loanAmount;

    private Integer age;
    private Double monthlyIncome;
    private EmployeeType employeeType;
    private String pinCode;
    private Boolean isEducated;
    private String certificates;
    private String collateral;
}