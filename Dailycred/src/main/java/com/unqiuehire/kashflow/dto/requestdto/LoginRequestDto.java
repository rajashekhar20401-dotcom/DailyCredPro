package com.unqiuehire.kashflow.dto.requestdto;

import com.unqiuehire.kashflow.constant.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    private String identifier; // borrower/lender phone, admin email
    private String password;
    private Role role;
}