package com.unqiuehire.kashflow.constant;
public enum BorrowerConstants {

    BORROWER_CREATED("Borrower created successfully"),
    BORROWER_UPDATED("Borrower updated successfully"),
    BORROWER_FOUND("Borrower fetched successfully"),
    BORROWERS_FOUND("Borrowers fetched successfully"),
    BORROWER_DELETED("Borrower deleted successfully"),
    BORROWER_NOT_FOUND("Borrower not found");

    private final String message;

    BorrowerConstants(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}