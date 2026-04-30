package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.dto.responsedto.EligibilityResultDto;
import com.unqiuehire.kashflow.dto.responsedto.RiskAnalysisResultDto;
import com.unqiuehire.kashflow.entity.Borrower;
import com.unqiuehire.kashflow.entity.Loan;
import com.unqiuehire.kashflow.service.EligibilityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EligibilityServiceImpl implements EligibilityService {

    @Override
    public EligibilityResultDto evaluate(Borrower borrower,
                                         Integer internalCreditScore,
                                         RiskAnalysisResultDto riskResult,
                                         List<Loan> loans) {

        int riskScore = riskResult == null || riskResult.getRiskScore() == null
                ? 100
                : riskResult.getRiskScore();

        int activeLoans = (int) loans.stream()
                .filter(loan -> !Boolean.TRUE.equals(loan.getIsClosed()))
                .count();

        EligibilityResultDto result = new EligibilityResultDto();

        // Hard rejection gates first
        if (Boolean.TRUE.equals(borrower.getFraudFlag()) || Boolean.TRUE.equals(borrower.getBlacklisted())) {
            result.setEligibilityTier("REJECTED");
            result.setEligibilityStatus("MANUAL_REVIEW_REQUIRED");
            result.setMaxEligibleLoanAmount(0.0);
            result.setCollateralRequired(true);
            result.setReason("Borrower is flagged for fraud or blacklisted");
            return result;
        }

        if (riskScore > 80) {
            result.setEligibilityTier("REJECTED");
            result.setEligibilityStatus("HIGH_RISK");
            result.setMaxEligibleLoanAmount(0.0);
            result.setCollateralRequired(true);
            result.setReason("Risk score is too high");
            return result;
        }

        if (activeLoans >= 3 && riskScore > 50) {
            result.setEligibilityTier("REJECTED");
            result.setEligibilityStatus("MANUAL_REVIEW_REQUIRED");
            result.setMaxEligibleLoanAmount(0.0);
            result.setCollateralRequired(true);
            result.setReason("Too many active loans with unstable repayment behavior");
            return result;
        }

        // Matrix-style evaluation
        if (internalCreditScore >= 75) {
            if (riskScore <= 20) {
                result.setEligibilityTier("A");
                result.setEligibilityStatus("APPROVED");
                result.setMaxEligibleLoanAmount(500000.0);
                result.setCollateralRequired(false);
                result.setReason("Strong profile and low risk");
            } else if (riskScore <= 40) {
                result.setEligibilityTier("B");
                result.setEligibilityStatus("APPROVED_WITH_LIMIT");
                result.setMaxEligibleLoanAmount(200000.0);
                result.setCollateralRequired(false);
                result.setReason("Strong profile but moderate repayment risk");
            } else if (riskScore <= 60) {
                result.setEligibilityTier("C");
                result.setEligibilityStatus("COLLATERAL_REQUIRED");
                result.setMaxEligibleLoanAmount(100000.0);
                result.setCollateralRequired(true);
                result.setReason("Strong profile but repayment behavior requires tighter control");
            } else {
                result.setEligibilityTier("MANUAL");
                result.setEligibilityStatus("MANUAL_REVIEW_REQUIRED");
                result.setMaxEligibleLoanAmount(50000.0);
                result.setCollateralRequired(true);
                result.setReason("High profile strength but risk is too elevated for automatic approval");
            }
            return result;
        }

        if (internalCreditScore >= 60) {
            if (riskScore <= 30) {
                result.setEligibilityTier("B");
                result.setEligibilityStatus("APPROVED_WITH_LIMIT");
                result.setMaxEligibleLoanAmount(200000.0);
                result.setCollateralRequired(false);
                result.setReason("Moderately strong profile with manageable risk");
            } else if (riskScore <= 50) {
                result.setEligibilityTier("C");
                result.setEligibilityStatus("COLLATERAL_REQUIRED");
                result.setMaxEligibleLoanAmount(100000.0);
                result.setCollateralRequired(true);
                result.setReason("Moderate profile and moderate risk, so collateral is required");
            } else {
                result.setEligibilityTier("MANUAL");
                result.setEligibilityStatus("MANUAL_REVIEW_REQUIRED");
                result.setMaxEligibleLoanAmount(50000.0);
                result.setCollateralRequired(true);
                result.setReason("Moderate profile but repayment risk is too high for direct approval");
            }
            return result;
        }

        if (internalCreditScore >= 45) {
            if (riskScore <= 40) {
                result.setEligibilityTier("C");
                result.setEligibilityStatus("APPROVED_SMALL_LIMIT");
                result.setMaxEligibleLoanAmount(100000.0);
                result.setCollateralRequired(true);
                result.setReason("Lower trust tier, allow only small-ticket loans with stronger controls");
            } else {
                result.setEligibilityTier("MANUAL");
                result.setEligibilityStatus("MANUAL_REVIEW_REQUIRED");
                result.setMaxEligibleLoanAmount(50000.0);
                result.setCollateralRequired(true);
                result.setReason("Weak profile with elevated risk");
            }
            return result;
        }

        result.setEligibilityTier("REJECTED");
        result.setEligibilityStatus("LOW_PROFILE_STRENGTH");
        result.setMaxEligibleLoanAmount(0.0);
        result.setCollateralRequired(true);
        result.setReason("Borrower profile strength is too low");
        return result;
    }
}