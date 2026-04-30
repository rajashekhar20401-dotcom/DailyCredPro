package com.unqiuehire.kashflow.dto.requestdto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LenderRequestDto {

    private String lenderName;
    private String dateOfBirth;
    private String password;
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
    private String termsVersion;
}