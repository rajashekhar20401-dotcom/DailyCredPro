package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.entity.Repayment;
import com.unqiuehire.kashflow.service.InternalCreditScoreService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InternalCreditScoreServiceImpl  implements InternalCreditScoreService {
    @Override
    public Integer calculateInternalCreditScore(Borrower borrower, List<Loan> loans, List<Repayment> repayments) {

        int score = 0;

        if(borrower.getKycCompletionPercent() != null){
            score += Math.min(25,borrower.getKycCompletionPercent()/4);
        }

        if (Boolean.TRUE.equals(borrower.getKycVerified())) score += 10;
        if (Boolean.TRUE.equals(borrower.getIncomeProofUploaded())) score += 8;

        if (borrower.getMonthlyIncome() != null) {
            if (borrower.getMonthlyIncome().doubleValue() >= 50000) score += 20;
            else if (borrower.getMonthlyIncome().doubleValue() >= 25000) score += 15;
            else if (borrower.getMonthlyIncome().doubleValue() >= 10000) score += 10;
            else score += 5;
        }

        String employmentType = safeLower(borrower.getEmploymentType());

        if (employmentType.contains("government")) score += 15;
        else if (employmentType.contains("private")) score += 12;
        else if (employmentType.contains("self")) score += 10;
        else if (employmentType.contains("business")) score += 10;
        else if (!employmentType.isEmpty()) score += 5;

        if (borrower.getYearsInCurrentWork() != null) {
            if (borrower.getYearsInCurrentWork() >= 5) score += 10;
            else if (borrower.getYearsInCurrentWork() >= 2) score += 6;
            else if (borrower.getYearsInCurrentWork() >= 1) score += 3;
        }

        if (Boolean.TRUE.equals(borrower.getHouseOwned())) score += 8;
        if (Boolean.TRUE.equals(borrower.getShopOwned())) score += 5;
        if (Boolean.TRUE.equals(borrower.getPropertyOwned())) score += 8;
        if (Boolean.TRUE.equals(borrower.getCollateralProvided())) score += 10;

        long goodClosures = loans.stream()
                .filter(loan -> Boolean.TRUE.equals(loan.getIsClosed()) && !Boolean.TRUE.equals(loan.getClosedLate()))
                .count();

        score += Math.min(10, (int) goodClosures * 2);

        if (borrower.getTotalMissedDays() != null) {
            score -= Math.min(10, borrower.getTotalMissedDays() / 10);
        }

        if (Boolean.TRUE.equals(borrower.getFraudFlag())) score -= 30;
        if (Boolean.TRUE.equals(borrower.getBlacklisted())) score -= 20;

        if (score < 0) score = 0;
        if (score > 100) score = 100;

        return score;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}

