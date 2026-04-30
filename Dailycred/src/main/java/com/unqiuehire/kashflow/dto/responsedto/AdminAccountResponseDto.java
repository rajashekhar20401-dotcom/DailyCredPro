package com.unqiuehire.kashflow.dto.responsedto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAccountResponseDto {
    private Long adminId;
    private String adminName;
    private String email;
    private Boolean active;
    private Boolean superAdmin;

    private Boolean notificationEnabled;
    private Boolean emailNotificationsEnabled;
}
