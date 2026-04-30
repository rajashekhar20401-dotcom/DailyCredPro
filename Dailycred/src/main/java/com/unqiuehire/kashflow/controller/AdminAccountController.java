package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.AdminAccountRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.AdminAccountResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.service.AdminAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @PostMapping
    public ApiResponse<AdminAccountResponseDto> createAdmin(@RequestBody AdminAccountRequestDto requestDto) {
        return adminAccountService.createAdmin(requestDto);
    }

    @GetMapping("/{adminId}")
    public ApiResponse<AdminAccountResponseDto> getAdminById(@PathVariable Long adminId) {
        return adminAccountService.getAdminById(adminId);
    }
}