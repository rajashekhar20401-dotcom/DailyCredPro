package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.AdminAccountRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.AdminAccountResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;

public interface AdminAccountService {
    ApiResponse<AdminAccountResponseDto> createAdmin(AdminAccountRequestDto requestDto);
    ApiResponse<AdminAccountResponseDto> getAdminById(Long adminId);
}
