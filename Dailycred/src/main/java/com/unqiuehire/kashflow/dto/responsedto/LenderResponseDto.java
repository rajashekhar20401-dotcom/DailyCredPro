package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LenderResponseDto {

    private Long lenderId;
    private String lenderName;
    private String dateOfBirth;
    private Boolean isActive;
    private String phoneNumber;
    private String pincode;
    private String address;
    private String aadharCardNumber;
    private String panCardNumber;

    private String email;
    private Boolean notificationEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean termsAccepted;
    private LocalDateTime termsAcceptedAt;
    private String termsVersion;
}