package com.unqiuehire.kashflow.constant;
import lombok.Getter;

@Getter
public enum LoanApplicationConstants {

    LOAN_APPLICATION_CREATED("Loan application created successfully"),
    LOAN_APPLICATION_FOUND("Loan application fetched successfully"),
    LOAN_APPLICATIONS_FOUND("Loan applications fetched successfully"),
    LOAN_APPLICATION_NOT_FOUND("Loan application not found"),
    LOAN_APPLICATION_CANCELLED("Loan application cancelled successfully");

    private final String message;

    LoanApplicationConstants(String message) {
        this.message = message;
    }

}