package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.dto.responsedto.RiskAnalysisResultDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.Repayment;
import com.unqiuehire.kashflow.service.RiskAnalysisService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskAnalysisServiceImpl implements RiskAnalysisService {
    @Override
    public RiskAnalysisResultDto analyze(Borrower borrower, List<Loan> loans, List<Repayment> repayments) {

        int totalMissedDays=loans.stream()
                .map(Loan::getMissedDaysCount)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();

        int totalPartialPayments = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPartialPayment()))
                .count();

        int totalAdvancePayments = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsAdvancePayment()))
                .count();

        int totalLatePayments = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsLatePayment()))
                .count();

        int totalPreClosures = (int) repayments.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPreClosure()))
                .count();

        int activeLoans = (int) loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .count();

        double currentOutstanding = loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .map(Loan::getRemainingAmount)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        int maxConsecutiveMissedDays = repayments.stream()
                .map(Repayment::getMissedDays)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        int risk=0;

        risk += totalMissedDays*2;
        risk += totalPartialPayments;
        risk += totalLatePayments * 2;
        risk += activeLoans * 5;

        if (currentOutstanding >= 200000) risk += 15;
        else if (currentOutstanding >= 100000) risk += 10;
        else if (currentOutstanding > 0) risk += 5;

        if (maxConsecutiveMissedDays >= 10) risk += 20;
        if (Boolean.TRUE.equals(borrower.getManualReviewFlag())) risk += 15;
        if (Boolean.TRUE.equals(borrower.getBlacklisted())) risk += 30;
        if (Boolean.TRUE.equals(borrower.getFraudFlag())) risk += 40;

        risk -= totalAdvancePayments;
        risk -= totalPreClosures * 4;

        if (risk < 0) risk = 0;
        if (risk > 100) risk = 100;

        String category;
        String recommendation;

        if (risk <= 20) {
            category = "LOW";
            recommendation = "Borrower shows relatively stable behavior";
        } else if (risk <= 40) {
            category = "MEDIUM";
            recommendation = "Borrower needs controlled exposure";
        } else if (risk <= 70) {
            category = "HIGH";
            recommendation = "Lend only with stronger restrictions or collateral";
        } else {
            category = "VERY_HIGH";
            recommendation = "Reject or require strict manual review";
        }

        RiskAnalysisResultDto dto = new RiskAnalysisResultDto();
        dto.setRiskScore(risk);
        dto.setRiskCategory(category);
        dto.setRecommendation(recommendation);
        return dto;
    }
}
