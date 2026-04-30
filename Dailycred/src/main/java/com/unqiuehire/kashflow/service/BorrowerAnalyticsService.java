package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerAnalyticsSummaryResponseDto;

public interface BorrowerAnalyticsService {

    ApiResponse<BorrowerAnalyticsSummaryResponseDto> getBorrowerSummary(Long borrowerId);
}

