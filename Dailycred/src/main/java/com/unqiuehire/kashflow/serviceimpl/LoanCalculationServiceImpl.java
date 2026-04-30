package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.service.LoanCalculationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LoanCalculationServiceImpl implements LoanCalculationService {

    @Override
    public BigDecimal getInterestDeductionPercent(BigDecimal sanctionedAmount) {
        if (sanctionedAmount.compareTo(BigDecimal.valueOf(100000)) < 0) {
            return BigDecimal.valueOf(10);
        } else if (sanctionedAmount.compareTo(BigDecimal.valueOf(100000)) >= 0
                && sanctionedAmount.compareTo(BigDecimal.valueOf(200000)) <= 0) {
            return BigDecimal.valueOf(7);
        } else if (sanctionedAmount.compareTo(BigDecimal.valueOf(300000)) > 0) {
            return BigDecimal.valueOf(3);
        }
        return BigDecimal.valueOf(5);
    }

    @Override
    public BigDecimal calculateUpfrontInterest(BigDecimal sanctionedAmount) {
        BigDecimal percent = getInterestDeductionPercent(sanctionedAmount);
        return calculateUpfrontInterest(sanctionedAmount, percent);
    }

    @Override
    public BigDecimal calculateDisbursedAmount(BigDecimal sanctionedAmount) {
        return sanctionedAmount.subtract(calculateUpfrontInterest(sanctionedAmount));
    }

    @Override
    public BigDecimal calculateUpfrontInterest(BigDecimal sanctionedAmount, BigDecimal percent) {
        if (percent == null) {
            percent = getInterestDeductionPercent(sanctionedAmount);
        }

        return sanctionedAmount.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateDisbursedAmount(BigDecimal sanctionedAmount, BigDecimal percent) {
        return sanctionedAmount.subtract(calculateUpfrontInterest(sanctionedAmount, percent));
    }

    @Override
    public BigDecimal calculateDailyDue(BigDecimal totalRepayableAmount, Integer tenureDays) {
        if (tenureDays == null || tenureDays <= 0) {
            throw new RuntimeException("Tenure days must be greater than zero");
        }

        return totalRepayableAmount.divide(BigDecimal.valueOf(tenureDays), 2, RoundingMode.HALF_UP);
    }
}