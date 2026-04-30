package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.responsedto.BorrowerRiskBreakdownResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LenderDashboardSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LenderTodayCollectionItemResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LoanPlanPerformanceResponseDto;
import com.unqiuehire.kashflow.service.LenderAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lender-analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LenderAnalyticsController {

    private final LenderAnalyticsService lenderAnalyticsService;

    @GetMapping("/{lenderId}/dashboard")
    public LenderDashboardSummaryResponseDto getDashboard(@PathVariable Long lenderId) {
        return lenderAnalyticsService.getDashboardSummary(lenderId);
    }

    @GetMapping("/{lenderId}/today-collections")
    public List<LenderTodayCollectionItemResponseDto> getTodayCollections(@PathVariable Long lenderId) {
        return lenderAnalyticsService.getTodayCollections(lenderId);
    }

    @GetMapping("/{lenderId}/loan-plan-performance")
    public List<LoanPlanPerformanceResponseDto> getLoanPlanPerformance(@PathVariable Long lenderId) {
        return lenderAnalyticsService.getLoanPlanPerformance(lenderId);
    }

    @GetMapping("/{lenderId}/borrower-risk-breakdown")
    public List<BorrowerRiskBreakdownResponseDto> getBorrowerRiskBreakdown(@PathVariable Long lenderId) {
        return lenderAnalyticsService.getBorrowerRiskBreakdown(lenderId);
    }
}
