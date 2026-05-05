package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.requestdto.LocationUpdateRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerLastKnownLocationResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.NearbyLenderResponseDto;

import java.util.List;

public interface LocationService {
    ApiResponse<String> updateBorrowerLocation(Long borrowerId, LocationUpdateRequestDto requestDto);
    ApiResponse<String> updateLenderLocation(Long lenderId, LocationUpdateRequestDto requestDto);
    ApiResponse<List<NearbyLenderResponseDto>> getNearbyLendersForBorrower(Long borrowerId, Double radiusKm);
    ApiResponse<BorrowerLastKnownLocationResponseDto> getBorrowerLastKnownLocationForLender(Long lenderId, Long borrowerId);
}
