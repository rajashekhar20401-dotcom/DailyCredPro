package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.LocationUpdateRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerLastKnownLocationResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.NearbyLenderResponseDto;
import com.unqiuehire.kashflow.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LocationController {

    private final LocationService locationService;

    @PutMapping("/borrowers/{borrowerId}")
    public ApiResponse<String> updateBorrowerLocation(@PathVariable Long borrowerId,
                                                      @RequestBody LocationUpdateRequestDto requestDto) {
        return locationService.updateBorrowerLocation(borrowerId, requestDto);
    }

    @PutMapping("/lenders/{lenderId}")
    public ApiResponse<String> updateLenderLocation(@PathVariable Long lenderId,
                                                    @RequestBody LocationUpdateRequestDto requestDto) {
        return locationService.updateLenderLocation(lenderId, requestDto);
    }

    @GetMapping("/borrowers/{borrowerId}/nearby-lenders")
    public ApiResponse<List<NearbyLenderResponseDto>> getNearbyLenders(@PathVariable Long borrowerId,
                                                                       @RequestParam(required = false) Double radiusKm) {
        return locationService.getNearbyLendersForBorrower(borrowerId, radiusKm);
    }

    @GetMapping("/lenders/{lenderId}/borrowers/{borrowerId}/last-known-location")
    public ApiResponse<BorrowerLastKnownLocationResponseDto> getBorrowerLastKnownLocation(@PathVariable Long lenderId,
                                                                                          @PathVariable Long borrowerId) {
        return locationService.getBorrowerLastKnownLocationForLender(lenderId, borrowerId);
    }
}