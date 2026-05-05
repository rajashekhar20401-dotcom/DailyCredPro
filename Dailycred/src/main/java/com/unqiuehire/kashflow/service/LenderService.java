package com.unqiuehire.kashflow.service;
import com.unqiuehire.kashflow.dto.requestdto.LenderRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LenderResponseDto;

import java.util.List;

public interface LenderService {

    ApiResponse<LenderResponseDto> createLender(LenderRequestDto lenderRequestDto);

    ApiResponse<LenderResponseDto> getLenderById(Long lenderId);

    ApiResponse<List<LenderResponseDto>> getAllLenders();

    ApiResponse<LenderResponseDto> updateLender(Long lenderId, LenderRequestDto lenderRequestDto);

    ApiResponse<String> deleteLender(Long lenderId);
}