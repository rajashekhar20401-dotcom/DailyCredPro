package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.dto.requestdto.LocationUpdateRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerLastKnownLocationResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.NearbyLenderResponseDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.repository.BorrowerRepository;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.repository.LoanRepository;
import com.unqiuehire.kashflow.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final BorrowerRepository borrowerRepository;
    private final LenderRepository lenderRepository;
    private final LoanRepository loanRepository;

    @Override
    @Transactional
    public ApiResponse<String> updateBorrowerLocation(Long borrowerId, LocationUpdateRequestDto requestDto) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        validateLocationRequest(requestDto);

        borrower.setCurrentLatitude(requestDto.getLatitude());
        borrower.setCurrentLongitude(requestDto.getLongitude());
        borrower.setLastKnownLatitude(requestDto.getLatitude());
        borrower.setLastKnownLongitude(requestDto.getLongitude());
        borrower.setLocationConsentGiven(Boolean.TRUE.equals(requestDto.getConsentGiven()));
        borrower.setLastLocationUpdatedAt(LocalDateTime.now());

        borrowerRepository.save(borrower);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower location updated successfully", "Borrower location updated successfully");
    }

    @Override
    @Transactional
    public ApiResponse<String> updateLenderLocation(Long lenderId, LocationUpdateRequestDto requestDto) {
        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() -> new RuntimeException("Lender not found"));

        validateLocationRequest(requestDto);

        lender.setCurrentLatitude(requestDto.getLatitude());
        lender.setCurrentLongitude(requestDto.getLongitude());
        lender.setLastKnownLatitude(requestDto.getLatitude());
        lender.setLastKnownLongitude(requestDto.getLongitude());
        lender.setLocationConsentGiven(Boolean.TRUE.equals(requestDto.getConsentGiven()));
        lender.setLastLocationUpdatedAt(LocalDateTime.now());

        lenderRepository.save(lender);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Lender location updated successfully", "Lender location updated successfully");
    }

    @Override
    public ApiResponse<List<NearbyLenderResponseDto>> getNearbyLendersForBorrower(Long borrowerId, Double radiusKm) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        if (borrower.getCurrentLatitude() == null || borrower.getCurrentLongitude() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower location not available", null);
        }

        double effectiveRadius = 30.0;
        List<Lender> allLenders = lenderRepository.findAll();
        List<NearbyLenderResponseDto> results = new ArrayList<>();

        for (Lender lender : allLenders) {
            if (Boolean.TRUE.equals(lender.getFrozen()) || Boolean.TRUE.equals(lender.getBlacklisted())) {
                continue;
            }

            if (lender.getCurrentLatitude() == null || lender.getCurrentLongitude() == null) {
                continue;
            }

            double distance = haversine(
                    borrower.getCurrentLatitude(),
                    borrower.getCurrentLongitude(),
                    lender.getCurrentLatitude(),
                    lender.getCurrentLongitude()
            );

            if (distance <= effectiveRadius) {
                NearbyLenderResponseDto dto = new NearbyLenderResponseDto();
                dto.setLenderId(lender.getLenderId());
                dto.setLenderName(lender.getLenderName());
                dto.setPincode(lender.getPincode());
                dto.setLatitude(lender.getCurrentLatitude());
                dto.setLongitude(lender.getCurrentLongitude());
                dto.setDistanceKm(Math.round(distance * 100.0) / 100.0);
                results.add(dto);
            }
        }

        return new ApiResponse<>(ApiStatus.SUCCESS, "Nearby lenders fetched successfully", results);
    }

    @Override
    public ApiResponse<BorrowerLastKnownLocationResponseDto> getBorrowerLastKnownLocationForLender(Long lenderId, Long borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found"));

        List<Loan> lenderLoans = loanRepository.findByLenderId(lenderId);

        boolean lenderHasBorrower = lenderLoans.stream()
                .anyMatch(loan -> loan.getBorrowerId().equals(borrowerId));

        if (!lenderHasBorrower) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender has no loan relationship with this borrower", null);
        }

        boolean borrowerIsDefaulter = lenderLoans.stream()
                .filter(loan -> loan.getBorrowerId().equals(borrowerId))
                .anyMatch(loan -> loan.getConsecutiveMissedDays() != null && loan.getConsecutiveMissedDays() >= 10);

        if (!borrowerIsDefaulter) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower is not in default condition for location access", null);
        }

        if (borrower.getLastKnownLatitude() == null || borrower.getLastKnownLongitude() == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Borrower last known location is not available", null);
        }

        BorrowerLastKnownLocationResponseDto dto = new BorrowerLastKnownLocationResponseDto();
        dto.setBorrowerId(borrower.getBorrowerId());
        dto.setBorrowerName(borrower.getBorrowerName());
        dto.setLatitude(borrower.getLastKnownLatitude());
        dto.setLongitude(borrower.getLastKnownLongitude());
        dto.setLastUpdatedAt(borrower.getLastLocationUpdatedAt());
        dto.setAccessReason("Borrower default threshold reached (10+ consecutive missed days)");

        return new ApiResponse<>(ApiStatus.SUCCESS, "Borrower last known location fetched successfully", dto);
    }

    private void validateLocationRequest(LocationUpdateRequestDto requestDto) {
        if (requestDto == null || requestDto.getLatitude() == null || requestDto.getLongitude() == null) {
            throw new RuntimeException("Latitude and longitude are required");
        }

        if (requestDto.getLatitude() < -90 || requestDto.getLatitude() > 90) {
            throw new RuntimeException("Latitude must be between -90 and 90");
        }

        if (requestDto.getLongitude() < -180 || requestDto.getLongitude() > 180) {
            throw new RuntimeException("Longitude must be between -180 and 180");
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }
}