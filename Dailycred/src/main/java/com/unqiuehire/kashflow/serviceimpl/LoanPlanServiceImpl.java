package com.unqiuehire.kashflow.serviceImpl;
import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.LoanPlanStatus;
import com.unqiuehire.kashflow.dto.requestdto.LoanPlanRequest;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanPlanResponseDto;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.entity.LoanPlan;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.repository.LoanPlanRepository;
import com.unqiuehire.kashflow.service.LoanPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanPlanServiceImpl implements LoanPlanService {

    @Autowired
    private LoanPlanRepository loanPlanRepository;

    @Autowired
    private LenderRepository lenderRepository;

    // ✅ CREATE
    @Override
    public ApiResponse<LoanPlanResponseDto> createLoanPlan(Long lenderId, LoanPlanRequest request) {

        Lender lender = lenderRepository.findById(lenderId).orElse(null);

        if (lender == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender not found", null);
        }

        LoanPlan loanPlan = new LoanPlan();

        loanPlan.setPlanName(request.getPlanName());
        loanPlan.setAmount(request.getAmount());
        loanPlan.setInterestPerDay(request.getInterestPerDay());
        loanPlan.setPenaltyAmount(request.getPenaltyAmount());
        loanPlan.setPlanDuration(request.getPlanDuration());
        loanPlan.setMinCibil(request.getMinCibil());
        loanPlan.setMinAge(request.getMinAge());
        loanPlan.setMaxAge(request.getMaxAge());
        loanPlan.setMinMonthlyIncome(request.getMinMonthlyIncome());
        loanPlan.setServicePinCode(request.getServicePinCode());
        loanPlan.setMaxActiveLoans(request.getMaxActiveLoans());
        loanPlan.setEmployeeType(request.getEmployeeType());
        loanPlan.setStatus(LoanPlanStatus.ACTIVE);
        loanPlan.setLender(lender);

        LoanPlan saved = loanPlanRepository.save(loanPlan);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Loan Plan Created", mapToResponse(saved));
    }

    // ✅ GET BY LENDER ID
    @Override
    public ApiResponse<List<LoanPlanResponseDto>> getLoanPlansByLenderId(Long lenderId) {

        Lender lender = lenderRepository.findById(lenderId).orElse(null);

        if (lender == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender not found", null);
        }

        List<LoanPlan> plans = loanPlanRepository.findByLender_LenderId(lenderId);

        if (plans.isEmpty()) {
            return new ApiResponse<>(ApiStatus.FAILURE, "No Loan Plans found", null);
        }

        List<LoanPlanResponseDto> responseList = plans.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(ApiStatus.SUCCESS, "Loan Plans fetched", responseList);
    }

    // ✅ UPDATE BY LENDER ID
    @Override
    public ApiResponse<LoanPlanResponseDto> updateLoanPlanByLenderId(Long lenderId, Long planId, LoanPlanRequest request) {

        Lender lender = lenderRepository.findById(lenderId).orElse(null);

        if (lender == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Lender not found", null);
        }

        LoanPlan loanPlan = loanPlanRepository.findById(planId).orElse(null);

        if (loanPlan == null) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Loan Plan not found", null);
        }

        // 🔥 Ensure plan belongs to lender
        if (!loanPlan.getLender().getLenderId().equals(lenderId)) {
            return new ApiResponse<>(ApiStatus.FAILURE, "Unauthorized: Plan not belongs to lender", null);
        }

        // update fields
        loanPlan.setPlanName(request.getPlanName());
        loanPlan.setAmount(request.getAmount());
        loanPlan.setInterestPerDay(request.getInterestPerDay());
        loanPlan.setPenaltyAmount(request.getPenaltyAmount());
        loanPlan.setPlanDuration(request.getPlanDuration());
        loanPlan.setMinCibil(request.getMinCibil());
        loanPlan.setMinAge(request.getMinAge());
        loanPlan.setMaxAge(request.getMaxAge());
        loanPlan.setMinMonthlyIncome(request.getMinMonthlyIncome());
        loanPlan.setServicePinCode(request.getServicePinCode());
        loanPlan.setMaxActiveLoans(request.getMaxActiveLoans());
        loanPlan.setEmployeeType(request.getEmployeeType());
        loanPlan.setStatus(LoanPlanStatus.ACTIVE);

        LoanPlan updated = loanPlanRepository.save(loanPlan);

        return new ApiResponse<>(ApiStatus.SUCCESS, "Loan Plan Updated", mapToResponse(updated));
    }

    // ✅ MAPPER
    private LoanPlanResponseDto mapToResponse(LoanPlan loanPlan) {

        LoanPlanResponseDto response = new LoanPlanResponseDto();

        response.setId(loanPlan.getId());
        response.setPlanName(loanPlan.getPlanName());
        response.setLenderId(loanPlan.getLender().getLenderId());
        response.setAmount(loanPlan.getAmount());
        response.setInterestPerDay(loanPlan.getInterestPerDay());
        response.setPenaltyAmount(loanPlan.getPenaltyAmount());
        response.setPlanDuration(loanPlan.getPlanDuration());
        response.setMinCibil(loanPlan.getMinCibil());
        response.setMinAge(loanPlan.getMinAge());
        response.setMaxAge(loanPlan.getMaxAge());
        response.setMinMonthlyIncome(loanPlan.getMinMonthlyIncome());
        response.setServicePinCode(loanPlan.getServicePinCode());
        response.setMaxActiveLoans(loanPlan.getMaxActiveLoans());
        response.setEmployeeType(loanPlan.getEmployeeType());
        response.setStatus(loanPlan.getStatus());

        return response;
    }
}