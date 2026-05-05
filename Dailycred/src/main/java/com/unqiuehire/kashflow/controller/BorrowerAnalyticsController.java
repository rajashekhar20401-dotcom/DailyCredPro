package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerAnalyticsSummaryResponseDto;
import com.unqiuehire.kashflow.service.BorrowerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/borrower-analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BorrowerAnalyticsController {

    private final BorrowerAnalyticsService borrowerAnalyticsService;

    @GetMapping("/{borrowerId}/summary")
    public ApiResponse<BorrowerAnalyticsSummaryResponseDto> getBorrowerSummary(@PathVariable Long borrowerId) {
        return borrowerAnalyticsService.getBorrowerSummary(borrowerId);
    }
}
