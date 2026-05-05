package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.dto.requestdto.AdminAccountRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.AdminAccountResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.entity.AdminAccount;
import com.unqiuehire.kashflow.repository.AdminAccountRepository;
import com.unqiuehire.kashflow.service.AdminAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements AdminAccountService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ApiResponse<AdminAccountResponseDto> createAdmin(AdminAccountRequestDto requestDto) {
        if (requestDto == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Admin request cannot be null", null);
        }

        if (isBlank(requestDto.getAdminName()) || isBlank(requestDto.getEmail()) || isBlank(requestDto.getPassword())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Admin name, email, and password are required", null);
        }

        if (adminAccountRepository.findByEmail(requestDto.getEmail().trim()).isPresent()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Admin email already exists", null);
        }

        AdminAccount admin = new AdminAccount();
        admin.setAdminName(requestDto.getAdminName().trim());
        admin.setEmail(requestDto.getEmail().trim());
        admin.setPassword(passwordEncoder.encode(requestDto.getPassword().trim()));
        admin.setActive(requestDto.getActive() == null ? true : requestDto.getActive());
        admin.setSuperAdmin(requestDto.getSuperAdmin() == null ? false : requestDto.getSuperAdmin());
        admin.setNotificationEnabled(requestDto.getNotificationEnabled() == null ? true : requestDto.getNotificationEnabled());
        admin.setEmailNotificationsEnabled(requestDto.getEmailNotificationsEnabled() == null ? true : requestDto.getEmailNotificationsEnabled());

        AdminAccount saved = adminAccountRepository.save(admin);
        return new ApiResponse<>(ApiStatus.SUCCESS, "Admin created successfully", map(saved));
    }

    @Override
    public ApiResponse<AdminAccountResponseDto> getAdminById(Long adminId) {
        return adminAccountRepository.findById(adminId)
                .map(admin -> new ApiResponse<>(ApiStatus.SUCCESS, "Admin found", map(admin)))
                .orElseGet(() -> new ApiResponse<>(ApiStatus.FAILURE, "Admin not found", null));
    }

    private AdminAccountResponseDto map(AdminAccount admin) {
        AdminAccountResponseDto dto = new AdminAccountResponseDto();
        dto.setAdminId(admin.getAdminId());
        dto.setAdminName(admin.getAdminName());
        dto.setEmail(admin.getEmail());
        dto.setActive(admin.getActive());
        dto.setSuperAdmin(admin.getSuperAdmin());
        dto.setNotificationEnabled(admin.getNotificationEnabled());
        dto.setEmailNotificationsEnabled(admin.getEmailNotificationsEnabled());
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}