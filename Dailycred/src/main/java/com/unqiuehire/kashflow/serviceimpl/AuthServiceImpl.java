package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.Role;
import com.unqiuehire.kashflow.dto.requestdto.LoginRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoginResponseDto;
import com.unqiuehire.kashflow.entity.AdminAccount;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.repository.AdminAccountRepository;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.security.JwtUtil;
import com.unqiuehire.kashflow.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final BorrowerRepository borrowerRepository;
    private final LenderRepository lenderRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(BorrowerRepository borrowerRepository,
                           LenderRepository lenderRepository,
                           AdminAccountRepository adminAccountRepository,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder) {
        this.borrowerRepository = borrowerRepository;
        this.lenderRepository = lenderRepository;
        this.adminAccountRepository = adminAccountRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ApiResponse<LoginResponseDto> login(LoginRequestDto dto) {
        if (dto == null || dto.getRole() == null || dto.getIdentifier() == null || dto.getPassword() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Role, identifier, and password are required", null);
        }

        return switch (dto.getRole()) {
            case BORROWER -> borrowerLogin(dto);
            case LENDER -> lenderLogin(dto);
            case ADMIN -> adminLogin(dto);
        };
    }

    private ApiResponse<LoginResponseDto> borrowerLogin(LoginRequestDto dto) {
        Optional<Borrower> optionalBorrower = borrowerRepository.findByPhoneNumber(dto.getIdentifier().trim());

        if (optionalBorrower.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower not found", null);
        }

        Borrower borrower = optionalBorrower.get();

        if (!Boolean.TRUE.equals(borrower.getIsActive())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower account is inactive", null);
        }

        if (Boolean.TRUE.equals(borrower.getFrozen())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower account is frozen", null);
        }

        if (!passwordEncoder.matches(dto.getPassword(), borrower.getPassword())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Invalid password", null);
        }

        String token = jwtUtil.generateToken(
                borrower.getBorrowerId(),
                borrower.getPhoneNumber(),
                Role.BORROWER
        );

        LoginResponseDto responseDto = new LoginResponseDto();
        responseDto.setToken(token);
        responseDto.setRole(Role.BORROWER);
        responseDto.setUserId(borrower.getBorrowerId());
        responseDto.setIdentifier(borrower.getPhoneNumber());
        responseDto.setDisplayName(borrower.getBorrowerName());

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower login successful", responseDto);
    }

    private ApiResponse<LoginResponseDto> lenderLogin(LoginRequestDto dto) {
        Optional<Lender> optionalLender = lenderRepository.findByPhoneNumber(dto.getIdentifier().trim());

        if (optionalLender.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender not found", null);
        }

        Lender lender = optionalLender.get();

        if (!Boolean.TRUE.equals(lender.getIsActive())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender account is inactive", null);
        }

        if (Boolean.TRUE.equals(lender.getFrozen())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender account is frozen", null);
        }

        if (!passwordEncoder.matches(dto.getPassword(), lender.getPassword())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Invalid password", null);
        }

        String token = jwtUtil.generateToken(
                lender.getLenderId(),
                lender.getPhoneNumber(),
                Role.LENDER
        );

        LoginResponseDto responseDto = new LoginResponseDto();
        responseDto.setToken(token);
        responseDto.setRole(Role.LENDER);
        responseDto.setUserId(lender.getLenderId());
        responseDto.setIdentifier(lender.getPhoneNumber());
        responseDto.setDisplayName(lender.getLenderName());

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender login successful", responseDto);
    }

    private ApiResponse<LoginResponseDto> adminLogin(LoginRequestDto dto) {
        Optional<AdminAccount> optionalAdmin = adminAccountRepository.findByEmail(dto.getIdentifier().trim());

        if (optionalAdmin.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Admin not found", null);
        }

        AdminAccount admin = optionalAdmin.get();

        if (!Boolean.TRUE.equals(admin.getActive())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Admin account is inactive", null);
        }

        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Invalid password", null);
        }

        String token = jwtUtil.generateToken(
                admin.getAdminId(),
                admin.getEmail(),
                Role.ADMIN
        );

        LoginResponseDto responseDto = new LoginResponseDto();
        responseDto.setToken(token);
        responseDto.setRole(Role.ADMIN);
        responseDto.setUserId(admin.getAdminId());
        responseDto.setIdentifier(admin.getEmail());
        responseDto.setDisplayName(admin.getAdminName());

        return new ApiResponse<>(ApiStatus.SUCCESS, "Admin login successful", responseDto);
    }
}