package com.unqiuehire.kashflow.constant;

import lombok.Getter;

@Getter
public enum LenderConstants {

    LENDER_CREATED("Lender created successfully"),
    LENDER_UPDATED("Lender updated successfully"),
    LENDER_FOUND("Lender fetched successfully"),
    LENDERS_FOUND("Lenders fetched successfully"),
    LENDER_DELETED("Lender deleted successfully"),
    LENDER_NOT_FOUND("Lender not found");

    private final String message;

    LenderConstants(String message) {
        this.message = message;
    }

}