package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.BorrowerConstants;
import com.unqiuehire.kashflow.dto.requestdto.BorrowerRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerResponseDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.service.BorrowerAnalyticsService;
import com.unqiuehire.kashflow.service.BorrowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowerServiceImpl implements BorrowerService {

    private final BorrowerRepository repo;
    private final BorrowerAnalyticsService borrowerAnalyticsService;
    private final PasswordEncoder passwordEncoder;
    // private final NotificationService notificationService;

    @Override
    public ApiResponse<BorrowerResponseDto> createBorrower(BorrowerRequestDto borrowerRequestDto) {

        String aadhar = normalize(borrowerRequestDto.getAadharCardNumber());
        String pan = normalize(borrowerRequestDto.getPanCardNumber());
        String phone = normalize(borrowerRequestDto.getPhoneNumber());
        String email = normalize(borrowerRequestDto.getEmail());

        ApiResponse<BorrowerResponseDto> validationFailure = validateBorrowerRequest(borrowerRequestDto);
        if (validationFailure != null) {
            return validationFailure;
        }

        if (aadhar != null && repo.findByAadharCardNumber(aadhar).isPresent()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Aadhaar number already exists", null);
        }

        if (pan != null && repo.findByPanCardNumber(pan).isPresent()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "PAN number already exists", null);
        }

        if (phone != null && repo.findByPhoneNumber(phone).isPresent()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Phone number already exists", null);
        }

        if (email != null && repo.findByEmail(email).isPresent()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Email already exists", null);
        }

        Borrower borrower = mapToEntity(borrowerRequestDto);
        borrower.setAadharCardNumber(aadhar);
        borrower.setPanCardNumber(pan);
        borrower.setPhoneNumber(phone);
        borrower.setEmail(email);

        Borrower savedBorrower = repo.save(borrower);

        // Single source of truth: derive score/risk/eligibility only from analytics service
        borrowerAnalyticsService.refreshBorrowerDerivedFields(savedBorrower.getBorrowerId());

        Borrower updatedBorrower = repo.findById(savedBorrower.getBorrowerId())
                .orElseThrow(() -> new RuntimeException("Borrower not found after creation"));

//        notificationService.createNotification(
//                NotificationTargetType.BORROWER,
//                updatedBorrower.getBorrowerId(),
//                NotificationChannelType.IN_APP,
//                "Borrower Account Created",
//                "Your borrower account has been created successfully."
//        );

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                BorrowerConstants.BORROWER_CREATED.getMessage(),
                mapToResponse(updatedBorrower)
        );
    }

    @Override
    public ApiResponse<BorrowerResponseDto> getBorrowerById(Long borrowerId) {
        Optional<Borrower> optionalBorrower = repo.findById(borrowerId);

        if (optionalBorrower.isEmpty()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    BorrowerConstants.BORROWER_NOT_FOUND.getMessage(),
                    null
            );
        }

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                BorrowerConstants.BORROWER_FOUND.getMessage(),
                mapToResponse(optionalBorrower.get())
        );
    }

    @Override
    public ApiResponse<List<BorrowerResponseDto>> getAllBorrowers() {
        List<BorrowerResponseDto> borrowerList = repo.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                BorrowerConstants.BORROWERS_FOUND.getMessage(),
                borrowerList
        );
    }

    @Override
    public ApiResponse<BorrowerResponseDto> updateBorrower(Long borrowerId, BorrowerRequestDto borrowerRequestDto) {
        Optional<Borrower> optionalBorrower = repo.findById(borrowerId);

        if (optionalBorrower.isEmpty()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    BorrowerConstants.BORROWER_NOT_FOUND.getMessage(),
                    null
            );
        }

        ApiResponse<BorrowerResponseDto> validationFailure = validateBorrowerRequest(borrowerRequestDto);
        if (validationFailure != null) {
            return validationFailure;
        }

        Borrower existingBorrower = optionalBorrower.get();

        String aadhar = normalize(borrowerRequestDto.getAadharCardNumber());
        String pan = normalize(borrowerRequestDto.getPanCardNumber());
        String phone = normalize(borrowerRequestDto.getPhoneNumber());
        String email = normalize(borrowerRequestDto.getEmail());

        if (aadhar != null) {
            Optional<Borrower> existingAadhar = repo.findByAadharCardNumber(aadhar);
            if (existingAadhar.isPresent() && !existingAadhar.get().getBorrowerId().equals(borrowerId)) {
                return new ApiResponse<>(ApiStatus.FAILURE, "Aadhaar number already exists", null);
            }
        }

        if (pan != null) {
            Optional<Borrower> existingPan = repo.findByPanCardNumber(pan);
            if (existingPan.isPresent() && !existingPan.get().getBorrowerId().equals(borrowerId)) {
                return new ApiResponse<>(ApiStatus.FAILURE, "PAN number already exists", null);
            }
        }

        if (phone != null) {
            Optional<Borrower> existingPhone = repo.findByPhoneNumber(phone);
            if (existingPhone.isPresent() && !existingPhone.get().getBorrowerId().equals(borrowerId)) {
                return new ApiResponse<>(ApiStatus.FAILURE, "Phone number already exists", null);
            }
        }

        if (email != null) {
            Optional<Borrower> existingEmail = repo.findByEmail(email);
            if (existingEmail.isPresent() && !existingEmail.get().getBorrowerId().equals(borrowerId)) {
                return new ApiResponse<>(ApiStatus.FAILURE, "Email already exists", null);
            }
        }

        existingBorrower.setBorrowerName(borrowerRequestDto.getBorrowerName().trim());
        existingBorrower.setDateOfBirth(LocalDate.parse(borrowerRequestDto.getDateOfBirth()));
        existingBorrower.setPassword(passwordEncoder.encode(borrowerRequestDto.getPassword().trim()));
        existingBorrower.setIsActive(borrowerRequestDto.getIsActive());
        existingBorrower.setPhoneNumber(phone);
        existingBorrower.setPincode(borrowerRequestDto.getPincode().trim());
        existingBorrower.setAddress(borrowerRequestDto.getAddress().trim());

        existingBorrower.setAadharCardNumber(aadhar);
        existingBorrower.setPanCardNumber(pan);

        existingBorrower.setMonthlyIncome(borrowerRequestDto.getMonthlyIncome());
        existingBorrower.setIncomeType(normalize(borrowerRequestDto.getIncomeType()));
        existingBorrower.setEmploymentType(normalize(borrowerRequestDto.getEmploymentType()));
        existingBorrower.setYearsInCurrentWork(borrowerRequestDto.getYearsInCurrentWork());
        existingBorrower.setDependentsCount(borrowerRequestDto.getDependentsCount());
        existingBorrower.setHouseOwned(defaultBoolean(borrowerRequestDto.getHouseOwned()));
        existingBorrower.setShopOwned(defaultBoolean(borrowerRequestDto.getShopOwned()));
        existingBorrower.setIncomeProofUploaded(defaultBoolean(borrowerRequestDto.getIncomeProofUploaded()));
        existingBorrower.setPropertyOwned(defaultBoolean(borrowerRequestDto.getPropertyOwned()));
        existingBorrower.setPropertyType(normalize(borrowerRequestDto.getPropertyType()));
        existingBorrower.setCollateralProvided(defaultBoolean(borrowerRequestDto.getCollateralProvided()));
        existingBorrower.setCollateralType(normalize(borrowerRequestDto.getCollateralType()));
        existingBorrower.setKycCompletionPercent(defaultInt(borrowerRequestDto.getKycCompletionPercent(), 0));
        existingBorrower.setKycVerified(defaultBoolean(borrowerRequestDto.getKycVerified()));

        existingBorrower.setEmail(email);
        existingBorrower.setNotificationEnabled(defaultTrue(borrowerRequestDto.getNotificationEnabled()));
        existingBorrower.setEmailNotificationsEnabled(defaultTrue(borrowerRequestDto.getEmailNotificationsEnabled()));
        existingBorrower.setTermsAccepted(defaultBoolean(borrowerRequestDto.getTermsAccepted()));

        if (Boolean.TRUE.equals(borrowerRequestDto.getTermsAccepted()) && existingBorrower.getTermsAcceptedAt() == null) {
            existingBorrower.setTermsAcceptedAt(LocalDateTime.now());
        }

        existingBorrower.setTermsVersion(normalize(borrowerRequestDto.getTermsVersion()));

        Borrower savedBorrower = repo.save(existingBorrower);

        borrowerAnalyticsService.refreshBorrowerDerivedFields(savedBorrower.getBorrowerId());

        Borrower updatedBorrower = repo.findById(savedBorrower.getBorrowerId())
                .orElseThrow(() -> new RuntimeException("Borrower not found after update"));

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                BorrowerConstants.BORROWER_UPDATED.getMessage(),
                mapToResponse(updatedBorrower)
        );
    }

    @Override
    public ApiResponse<String> deleteBorrower(Long borrowerId) {
        Optional<Borrower> optionalBorrower = repo.findById(borrowerId);

        if (optionalBorrower.isEmpty()) {
            return new ApiResponse<>(
                    ApiStatus.FAILURE,
                    BorrowerConstants.BORROWER_NOT_FOUND.getMessage(),
                    null
            );
        }

        repo.deleteById(borrowerId);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                BorrowerConstants.BORROWER_DELETED.getMessage(),
                "Deleted borrower with id: " + borrowerId
        );
    }

    private BorrowerResponseDto mapToResponse(Borrower borrower) {
        BorrowerResponseDto responseDto = new BorrowerResponseDto();

        responseDto.setBorrowerId(borrower.getBorrowerId());
        responseDto.setBorrowerName(borrower.getBorrowerName());
        responseDto.setDateOfBirth(String.valueOf(borrower.getDateOfBirth()));
        responseDto.setIsActive(borrower.getIsActive());
        responseDto.setPhoneNumber(borrower.getPhoneNumber());
        responseDto.setPincode(borrower.getPincode());
        responseDto.setAddress(borrower.getAddress());
        responseDto.setCibil(borrower.getCibil());
        responseDto.setAadharCardNumber(borrower.getAadharCardNumber());
        responseDto.setPanCardNumber(borrower.getPanCardNumber());

        responseDto.setMonthlyIncome(borrower.getMonthlyIncome());
        responseDto.setIncomeType(borrower.getIncomeType());
        responseDto.setEmploymentType(borrower.getEmploymentType());
        responseDto.setYearsInCurrentWork(borrower.getYearsInCurrentWork());
        responseDto.setDependentsCount(borrower.getDependentsCount());
        responseDto.setHouseOwned(borrower.getHouseOwned());
        responseDto.setShopOwned(borrower.getShopOwned());
        responseDto.setIncomeProofUploaded(borrower.getIncomeProofUploaded());
        responseDto.setPropertyOwned(borrower.getPropertyOwned());
        responseDto.setPropertyType(borrower.getPropertyType());
        responseDto.setCollateralProvided(borrower.getCollateralProvided());
        responseDto.setCollateralType(borrower.getCollateralType());
        responseDto.setKycCompletionPercent(borrower.getKycCompletionPercent());
        responseDto.setKycVerified(borrower.getKycVerified());

        responseDto.setInternalCreditScore(borrower.getInternalCreditScore());
        responseDto.setRiskScore(borrower.getRiskScore());
        responseDto.setEligibilityScore(borrower.getEligibilityScore());
        responseDto.setRiskCategory(borrower.getRiskCategory());
        responseDto.setEligibilityTier(borrower.getEligibilityTier());
        responseDto.setEligibilityStatus(borrower.getEligibilityStatus());
        responseDto.setMaxEligibleLoanAmount(borrower.getMaxEligibleLoanAmount());
        responseDto.setCurrentOutstandingAmount(borrower.getCurrentOutstandingAmount());
        responseDto.setActiveLoanCount(borrower.getActiveLoanCount());
        responseDto.setTotalLoansTaken(borrower.getTotalLoansTaken());
        responseDto.setLoansClosedSuccessfully(borrower.getLoansClosedSuccessfully());
        responseDto.setLoansClosedEarly(borrower.getLoansClosedEarly());
        responseDto.setTotalMissedDays(borrower.getTotalMissedDays());
        responseDto.setTotalPartialDays(borrower.getTotalPartialDays());
        responseDto.setTotalAdvanceDays(borrower.getTotalAdvanceDays());
        responseDto.setTotalLatePayments(borrower.getTotalLatePayments());
        responseDto.setMaxConsecutiveMissedDays(borrower.getMaxConsecutiveMissedDays());
        responseDto.setDefaultedLoanCount(borrower.getDefaultedLoanCount());
        responseDto.setFraudFlag(borrower.getFraudFlag());
        responseDto.setManualReviewFlag(borrower.getManualReviewFlag());
        responseDto.setBlacklisted(borrower.getBlacklisted());

        responseDto.setEmail(borrower.getEmail());
        responseDto.setNotificationEnabled(borrower.getNotificationEnabled());
        responseDto.setEmailNotificationsEnabled(borrower.getEmailNotificationsEnabled());
        responseDto.setTermsAccepted(borrower.getTermsAccepted());
        responseDto.setTermsVersion(borrower.getTermsVersion());
        responseDto.setTermsAcceptedAt(borrower.getTermsAcceptedAt());

        return responseDto;
    }

    private Borrower mapToEntity(BorrowerRequestDto borrowerRequestDto) {
        Borrower borrower = new Borrower();

        borrower.setBorrowerName(borrowerRequestDto.getBorrowerName().trim());
        borrower.setDateOfBirth(LocalDate.parse(borrowerRequestDto.getDateOfBirth()));
        borrower.setPassword(passwordEncoder.encode(borrowerRequestDto.getPassword().trim()));
        borrower.setIsActive(borrowerRequestDto.getIsActive());
        borrower.setPhoneNumber(normalize(borrowerRequestDto.getPhoneNumber()));
        borrower.setPincode(borrowerRequestDto.getPincode().trim());
        borrower.setAddress(borrowerRequestDto.getAddress().trim());

        borrower.setAadharCardNumber(normalize(borrowerRequestDto.getAadharCardNumber()));
        borrower.setPanCardNumber(normalize(borrowerRequestDto.getPanCardNumber()));

        borrower.setMonthlyIncome(borrowerRequestDto.getMonthlyIncome());
        borrower.setIncomeType(normalize(borrowerRequestDto.getIncomeType()));
        borrower.setEmploymentType(normalize(borrowerRequestDto.getEmploymentType()));
        borrower.setYearsInCurrentWork(borrowerRequestDto.getYearsInCurrentWork());
        borrower.setDependentsCount(borrowerRequestDto.getDependentsCount());
        borrower.setHouseOwned(defaultBoolean(borrowerRequestDto.getHouseOwned()));
        borrower.setShopOwned(defaultBoolean(borrowerRequestDto.getShopOwned()));
        borrower.setIncomeProofUploaded(defaultBoolean(borrowerRequestDto.getIncomeProofUploaded()));
        borrower.setPropertyOwned(defaultBoolean(borrowerRequestDto.getPropertyOwned()));
        borrower.setPropertyType(normalize(borrowerRequestDto.getPropertyType()));
        borrower.setCollateralProvided(defaultBoolean(borrowerRequestDto.getCollateralProvided()));
        borrower.setCollateralType(normalize(borrowerRequestDto.getCollateralType()));
        borrower.setKycCompletionPercent(defaultInt(borrowerRequestDto.getKycCompletionPercent(), 0));
        borrower.setKycVerified(defaultBoolean(borrowerRequestDto.getKycVerified()));

        borrower.setEmail(normalize(borrowerRequestDto.getEmail()));
        borrower.setNotificationEnabled(defaultTrue(borrowerRequestDto.getNotificationEnabled()));
        borrower.setEmailNotificationsEnabled(defaultTrue(borrowerRequestDto.getEmailNotificationsEnabled()));
        borrower.setTermsAccepted(defaultBoolean(borrowerRequestDto.getTermsAccepted()));
        borrower.setTermsAcceptedAt(Boolean.TRUE.equals(borrowerRequestDto.getTermsAccepted()) ? LocalDateTime.now() : null);
        borrower.setTermsVersion(normalize(borrowerRequestDto.getTermsVersion()));

        // Safe defaults for persisted derived/cache fields.
        borrower.setCibil(0);
        borrower.setInternalCreditScore(0);
        borrower.setRiskScore(0);
        borrower.setRiskCategory("UNKNOWN");
        borrower.setEligibilityScore(0);
        borrower.setEligibilityTier("UNASSIGNED");
        borrower.setEligibilityStatus("PENDING_REVIEW");
        borrower.setMaxEligibleLoanAmount(BigDecimal.ZERO);
        borrower.setCurrentOutstandingAmount(BigDecimal.ZERO);
        borrower.setActiveLoanCount(0);
        borrower.setTotalLoansTaken(0);
        borrower.setLoansClosedSuccessfully(0);
        borrower.setLoansClosedEarly(0);
        borrower.setTotalMissedDays(0);
        borrower.setTotalPartialDays(0);
        borrower.setTotalAdvanceDays(0);
        borrower.setTotalLatePayments(0);
        borrower.setMaxConsecutiveMissedDays(0);
        borrower.setDefaultedLoanCount(0);

        return borrower;
    }

    private ApiResponse<BorrowerResponseDto> validateBorrowerRequest(BorrowerRequestDto borrowerRequestDto) {
        if (borrowerRequestDto == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower request cannot be null", null);
        }

        if (isBlank(borrowerRequestDto.getBorrowerName())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower name is required", null);
        }

        if (isBlank(borrowerRequestDto.getDateOfBirth())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Date of birth is required", null);
        }

        if (isBlank(borrowerRequestDto.getPassword())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Password is required", null);
        }

        if (borrowerRequestDto.getIsActive() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Active status is required", null);
        }

        if (isBlank(borrowerRequestDto.getPhoneNumber())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Phone number is required", null);
        }

        if (isBlank(borrowerRequestDto.getPincode())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Pincode is required", null);
        }

        if (isBlank(borrowerRequestDto.getAddress())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Address is required", null);
        }

        if (isBlank(borrowerRequestDto.getEmail())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Email is required", null);
        }

        if (borrowerRequestDto.getTermsAccepted() == null || !borrowerRequestDto.getTermsAccepted()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Terms must be accepted", null);
        }

        String aadhar = normalize(borrowerRequestDto.getAadharCardNumber());
        String pan = normalize(borrowerRequestDto.getPanCardNumber());

        if ((aadhar == null || aadhar.isEmpty()) && (pan == null || pan.isEmpty())) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Either Aadhaar or PAN must be provided", null);
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

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Boolean defaultTrue(Boolean value) {
        return value == null ? true : value;
    }
}