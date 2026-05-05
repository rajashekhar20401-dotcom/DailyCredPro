package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.dto.responsedto.BorrowerRiskBreakdownResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LenderDashboardSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LenderTodayCollectionItemResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LoanPlanPerformanceResponseDto;

import java.util.List;

public interface LenderAnalyticsService {
    LenderDashboardSummaryResponseDto getDashboardSummary(Long lenderId);
    List<LenderTodayCollectionItemResponseDto> getTodayCollections(Long lenderId);
    List<LoanPlanPerformanceResponseDto> getLoanPlanPerformance(Long lenderId);
    List<BorrowerRiskBreakdownResponseDto> getBorrowerRiskBreakdown(Long lenderId);
}
