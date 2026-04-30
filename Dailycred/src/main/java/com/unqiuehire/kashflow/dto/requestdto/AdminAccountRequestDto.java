package com.unqiuehire.kashflow.dto.requestdto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAccountRequestDto {
    private String adminName;
    private String email;
    private String password;
    private Boolean active;
    private Boolean superAdmin;

    private Boolean notificationEnabled;
    private Boolean emailNotificationsEnabled;
}
