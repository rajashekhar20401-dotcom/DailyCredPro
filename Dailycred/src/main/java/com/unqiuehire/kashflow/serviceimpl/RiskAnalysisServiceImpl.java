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

        int totalMissedDays = safeInt(borrower.getTotalMissedDays());
        int totalLatePayments = safeInt(borrower.getTotalLatePayments());
        int totalPartialPayments = safeInt(borrower.getTotalPartialDays());
        int totalAdvancePayments = safeInt(borrower.getTotalAdvanceDays());
        int maxConsecutiveMissedDays = safeInt(borrower.getMaxConsecutiveMissedDays());
        int defaultedLoanCount = safeInt(borrower.getDefaultedLoanCount());
        int activeLoanCount = safeInt(borrower.getActiveLoanCount());

        int closedSuccessfully = safeInt(borrower.getLoansClosedSuccessfully());
        int closedEarly = safeInt(borrower.getLoansClosedEarly());

        boolean fraudFlag = Boolean.TRUE.equals(borrower.getFraudFlag());
        boolean blacklisted = Boolean.TRUE.equals(borrower.getBlacklisted());
        boolean kycVerified = Boolean.TRUE.equals(borrower.getKycVerified());
        boolean incomeProofUploaded = Boolean.TRUE.equals(borrower.getIncomeProofUploaded());

        int riskScore = 0;

        // Negative repayment behaviour increases risk
        riskScore += Math.min(30, totalMissedDays * 2);
        riskScore += Math.min(15, totalLatePayments * 2);
        riskScore += Math.min(12, totalPartialPayments);
        riskScore += Math.min(20, maxConsecutiveMissedDays * 3);
        riskScore += Math.min(30, defaultedLoanCount * 15);
        riskScore += Math.min(12, activeLoanCount * 4);

        // Serious flags sharply increase risk
        if (fraudFlag) {
            riskScore += 35;
        }

        if (blacklisted) {
            riskScore += 45;
        }

        // Good behaviour reduces risk
        riskScore -= Math.min(10, totalAdvancePayments);
        riskScore -= Math.min(10, closedSuccessfully);
        riskScore -= Math.min(8, closedEarly * 2);

        if (kycVerified) {
            riskScore -= 5;
        }

        if (incomeProofUploaded) {
            riskScore -= 5;
        }

        if (riskScore < 0) {
            riskScore = 0;
        }

        if (riskScore > 100) {
            riskScore = 100;
        }

        String riskCategory;
        if (riskScore <= 24) {
            riskCategory = "LOW";
        } else if (riskScore <= 59) {
            riskCategory = "MEDIUM";
        } else {
            riskCategory = "HIGH";
        }

        String reason = buildReason(
                riskCategory,
                totalMissedDays,
                totalLatePayments,
                totalPartialPayments,
                maxConsecutiveMissedDays,
                defaultedLoanCount,
                fraudFlag,
                blacklisted,
                totalAdvancePayments,
                closedSuccessfully,
                closedEarly
        );

        RiskAnalysisResultDto dto = new RiskAnalysisResultDto();
        dto.setRiskScore(riskScore);
        dto.setRiskCategory(riskCategory);
        return dto;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildReason(
            String riskCategory,
            int totalMissedDays,
            int totalLatePayments,
            int totalPartialPayments,
            int maxConsecutiveMissedDays,
            int defaultedLoanCount,
            boolean fraudFlag,
            boolean blacklisted,
            int totalAdvancePayments,
            int closedSuccessfully,
            int closedEarly
    ) {
        if (blacklisted) {
            return "Borrower is blacklisted, so risk is automatically high.";
        }

        if (fraudFlag) {
            return "Fraud flag is present, so borrower requires strict caution.";
        }

        if ("HIGH".equals(riskCategory)) {
            if (defaultedLoanCount > 0) {
                return "High risk due to previous defaults and weak repayment behaviour.";
            }
            if (maxConsecutiveMissedDays >= 5 || totalMissedDays >= 10) {
                return "High risk due to repeated missed repayments.";
            }
            return "High risk due to weak repayment consistency.";
        }

        if ("MEDIUM".equals(riskCategory)) {
            if (totalLatePayments > 0 || totalPartialPayments > 0) {
                return "Medium risk because borrower has mixed repayment behaviour.";
            }
            return "Medium risk due to moderate repayment uncertainty.";
        }

        if (closedSuccessfully > 0 || closedEarly > 0 || totalAdvancePayments > 0) {
            return "Low risk because borrower has shown good repayment behaviour.";
        }
        return "Low risk based on current borrower profile and repayment record.";
    }
}