package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponseDto {
    private String token;
    private Role role;
    private Long userId;
    private String identifier;
    private String displayName;
}