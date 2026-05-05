package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.LoginRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoginResponseDto;

public interface AuthService {
    ApiResponse<LoginResponseDto> login(LoginRequestDto dto);
}