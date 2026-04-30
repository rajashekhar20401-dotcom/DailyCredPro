package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.LenderConstants;
import com.unqiuehire.kashflow.dto.requestdto.LenderRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LenderResponseDto;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.service.LenderService;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LenderServiceImpl implements LenderService {


    private final LenderRepository lenderRepository;

    private final PasswordEncoder passwordEncoder;

//    //public LenderServiceImpl(PasswordEncoder passwordEncoder) {
//        this.passwordEncoder = passwordEncoder;
//    }

    @Override
    public ApiResponse<LenderResponseDto> createLender(LenderRequestDto lenderRequestDto) {

        ApiResponse<LenderResponseDto> validationFailure = validateLenderRequest(lenderRequestDto);
        if (validationFailure != null) {
            return validationFailure;
        }

        String aadhar = normalize(lenderRequestDto.getAadharCardNumber());
        String pan = normalize(lenderRequestDto.getPanCardNumber());
        String phone = normalize(lenderRequestDto.getPhoneNumber());
        String email = normalize(lenderRequestDto.getEmail());

        boolean hasPhone = phone != null && !phone.isEmpty();
        boolean hasAadhar = aadhar != null && !aadhar.isEmpty();
        boolean hasPan = pan != null && !pan.isEmpty();

        if (hasPhone && lenderRepository.findByPhoneNumber(phone).isPresent()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Mobile number already exists",
                    null
            );
        }

        if (email != null && lenderRepository.findByEmail(email).isPresent()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Email already exists",
                    null
            );
        }

        if (hasAadhar && hasPan) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Please provide either Aadhar card or PAN card, not both.",
                    null
            );
        }

        if (!hasAadhar && !hasPan) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Please provide either Aadhar card or PAN card.",
                    null
            );
        }

        if (hasAadhar && lenderRepository.existsByAadharCardNumber(aadhar)) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Aadhar card already exists.",
                    null
            );
        }

        if (hasPan && lenderRepository.existsByPanCardNumber(pan)) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "PAN card already exists.",
                    null
            );
        }

        Lender lender = new Lender();
        lender.setLenderName(lenderRequestDto.getLenderName());
        lender.setDateOfBirth(LocalDate.parse(lenderRequestDto.getDateOfBirth()));
        lender.setPassword(passwordEncoder.encode(lenderRequestDto.getPassword()));
        lender.setIsActive(lenderRequestDto.getIsActive());
        lender.setPhoneNumber(phone);
        lender.setPincode(lenderRequestDto.getPincode());
        lender.setAddress(lenderRequestDto.getAddress());

        // new fields
        lender.setEmail(email);
        lender.setNotificationEnabled(defaultTrue(lenderRequestDto.getNotificationEnabled()));
        lender.setEmailNotificationsEnabled(defaultTrue(lenderRequestDto.getEmailNotificationsEnabled()));
        lender.setTermsAccepted(defaultBoolean(lenderRequestDto.getTermsAccepted()));
        lender.setTermsAcceptedAt(Boolean.TRUE.equals(lenderRequestDto.getTermsAccepted()) ? LocalDateTime.now() : null);
        lender.setTermsVersion(normalize(lenderRequestDto.getTermsVersion()));

        if (hasAadhar) {
            lender.setAadharCardNumber(aadhar);
            lender.setPanCardNumber(null);
        } else {
            lender.setPanCardNumber(pan);
            lender.setAadharCardNumber(null);
        }

        Lender savedLender = lenderRepository.save(lender);
        LenderResponseDto responseDto = mapToResponseDto(savedLender);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                LenderConstants.LENDER_CREATED.getMessage(),
                responseDto
        );
    }

    @Override
    public ApiResponse<LenderResponseDto> getLenderById(Long lenderId) {
        Optional<Lender> optionalLender = lenderRepository.findById(lenderId);

        if (optionalLender.isEmpty()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    LenderConstants.LENDER_NOT_FOUND.getMessage(),
                    null
            );
        }

        Lender lender = optionalLender.get();
        LenderResponseDto responseDto = mapToResponseDto(lender);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                LenderConstants.LENDER_FOUND.getMessage(),
                responseDto
        );
    }

    @Override
    public ApiResponse<List<LenderResponseDto>> getAllLenders() {
        List<Lender> lenderList = lenderRepository.findAll();
        List<LenderResponseDto> responseDtoList = new ArrayList<>();

        for (Lender lender : lenderList) {
            responseDtoList.add(mapToResponseDto(lender));
        }

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                LenderConstants.LENDERS_FOUND.getMessage(),
                responseDtoList
        );
    }

    @Override
    public ApiResponse<LenderResponseDto> updateLender(Long lenderId, LenderRequestDto lenderRequestDto) {
        Optional<Lender> optionalLender = lenderRepository.findById(lenderId);

        if (optionalLender.isEmpty()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    LenderConstants.LENDER_NOT_FOUND.getMessage(),
                    null
            );
        }

        ApiResponse<LenderResponseDto> validationFailure = validateLenderRequest(lenderRequestDto);
        if (validationFailure != null) {
            return validationFailure;
        }

        Lender lender = optionalLender.get();

        String phone = normalize(lenderRequestDto.getPhoneNumber());
        String email = normalize(lenderRequestDto.getEmail());
        String aadhar = normalize(lenderRequestDto.getAadharCardNumber());
        String pan = normalize(lenderRequestDto.getPanCardNumber());

        boolean hasPhone = phone != null && !phone.isEmpty();
        boolean hasAadhar = aadhar != null && !aadhar.isEmpty();
        boolean hasPan = pan != null && !pan.isEmpty();

        if (hasPhone) {
            Optional<Lender> existingPhone = lenderRepository.findByPhoneNumber(phone);
            if (existingPhone.isPresent() && !existingPhone.get().getLenderId().equals(lenderId)) {
                return new ApiResponse<>(
                        ApiStatus.FAILURE,
                        "Phone number already exists",
                        null
                );
            }
        }

        if (email != null) {
            Optional<Lender> existingEmail = lenderRepository.findByEmail(email);
            if (existingEmail.isPresent() && !existingEmail.get().getLenderId().equals(lenderId)) {
                return new ApiResponse<>(
                        ApiStatus.FAILURE,
                        "Email already exists",
                        null
                );
            }
        }

        if (hasAadhar && hasPan) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Please provide either Aadhar card or PAN card, not both.",
                    null
            );
        }

        if (!hasAadhar && !hasPan) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Please provide either Aadhar card or PAN card.",
                    null
            );
        }

        if (hasAadhar
                && !aadhar.equals(lender.getAadharCardNumber())
                && lenderRepository.existsByAadharCardNumber(aadhar)) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "Aadhar card already exists.",
                    null
            );
        }

        if (hasPan
                && !pan.equals(lender.getPanCardNumber())
                && lenderRepository.existsByPanCardNumber(pan)) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    "PAN card already exists.",
                    null
            );
        }

        lender.setLenderName(lenderRequestDto.getLenderName());
        lender.setDateOfBirth(LocalDate.parse(lenderRequestDto.getDateOfBirth()));
        lender.setPassword(passwordEncoder.encode(lenderRequestDto.getPassword()));
        lender.setIsActive(lenderRequestDto.getIsActive());
        lender.setPhoneNumber(phone);
        lender.setPincode(lenderRequestDto.getPincode());
        lender.setAddress(lenderRequestDto.getAddress());

        // new fields
        lender.setEmail(email);
        lender.setNotificationEnabled(defaultTrue(lenderRequestDto.getNotificationEnabled()));
        lender.setEmailNotificationsEnabled(defaultTrue(lenderRequestDto.getEmailNotificationsEnabled()));
        lender.setTermsAccepted(defaultBoolean(lenderRequestDto.getTermsAccepted()));
        if (Boolean.TRUE.equals(lenderRequestDto.getTermsAccepted()) && lender.getTermsAcceptedAt() == null) {
            lender.setTermsAcceptedAt(LocalDateTime.now());
        }
        lender.setTermsVersion(normalize(lenderRequestDto.getTermsVersion()));

        if (hasAadhar) {
            lender.setAadharCardNumber(aadhar);
            lender.setPanCardNumber(null);
        } else {
            lender.setPanCardNumber(pan);
            lender.setAadharCardNumber(null);
        }

        Lender updatedLender = lenderRepository.save(lender);
        LenderResponseDto responseDto = mapToResponseDto(updatedLender);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                LenderConstants.LENDER_UPDATED.getMessage(),
                responseDto
        );
    }

    @Override
    public ApiResponse<String> deleteLender(Long lenderId) {
        Optional<Lender> optionalLender = lenderRepository.findById(lenderId);

        if (optionalLender.isEmpty()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    LenderConstants.LENDER_NOT_FOUND.getMessage(),
                    null
            );
        }

        lenderRepository.deleteById(lenderId);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                LenderConstants.LENDER_DELETED.getMessage(),
                "Deleted lender with id: " + lenderId
        );
    }

    private LenderResponseDto mapToResponseDto(Lender lender) {
        LenderResponseDto responseDto = new LenderResponseDto();
        responseDto.setLenderId(lender.getLenderId());
        responseDto.setLenderName(lender.getLenderName());
        responseDto.setDateOfBirth(String.valueOf(lender.getDateOfBirth()));
        responseDto.setIsActive(lender.getIsActive());
        responseDto.setPhoneNumber(lender.getPhoneNumber());
        responseDto.setPincode(lender.getPincode());
        responseDto.setAddress(lender.getAddress());
        responseDto.setAadharCardNumber(lender.getAadharCardNumber());
        responseDto.setPanCardNumber(lender.getPanCardNumber());

        // new fields
        responseDto.setEmail(lender.getEmail());
        responseDto.setNotificationEnabled(lender.getNotificationEnabled());
        responseDto.setEmailNotificationsEnabled(lender.getEmailNotificationsEnabled());
        responseDto.setTermsAccepted(lender.getTermsAccepted());
        responseDto.setTermsVersion(lender.getTermsVersion());
        responseDto.setTermsAcceptedAt(lender.getTermsAcceptedAt());

        return responseDto;
    }

    private ApiResponse<LenderResponseDto> validateLenderRequest(LenderRequestDto lenderRequestDto) {
        if (lenderRequestDto == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender request cannot be null", null);
        }

        if (isBlank(lenderRequestDto.getLenderName())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender name is required", null);
        }

        if (isBlank(lenderRequestDto.getDateOfBirth())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Date of birth is required", null);
        }

        if (isBlank(lenderRequestDto.getPassword())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Password is required", null);
        }

        if (lenderRequestDto.getIsActive() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Active status is required", null);
        }

        if (isBlank(lenderRequestDto.getPhoneNumber())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Phone number is required", null);
        }

        if (isBlank(lenderRequestDto.getPincode())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Pincode is required", null);
        }

        if (isBlank(lenderRequestDto.getAddress())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Address is required", null);
        }

        if (isBlank(lenderRequestDto.getEmail())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Email is required", null);
        }

        if (lenderRequestDto.getTermsAccepted() == null || !lenderRequestDto.getTermsAccepted()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Terms must be accepted", null);
        }

        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Boolean defaultBoolean(Boolean value) {
        return value != null && value;
    }

    private Boolean defaultTrue(Boolean value) {
        return value == null ? true : value;
    }
}