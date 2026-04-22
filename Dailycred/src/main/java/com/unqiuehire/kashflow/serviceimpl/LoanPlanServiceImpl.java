package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.constant.LoanPlanStatus;
import com.unqiuehire.kashflow.dto.requestdto.LoanPlanRequest;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LoanPlanResponseDto;
import com.unqiuehire.kashflow.entity.Lender;
import com.unqiuehire.kashflow.entity.LoanPlan;
import com.unqiuehire.kashflow.exception.ResourceNotFoundException;
import com.unqiuehire.kashflow.repository.LenderRepository;
import com.unqiuehire.kashflow.repository.LoanPlanRepository;
import com.unqiuehire.kashflow.service.LoanPlanService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanPlanServiceImpl implements LoanPlanService {

    private final LoanPlanRepository repository;
    private final LenderRepository lenderRepository;

    // CREATE LOAN PLAN UNDER LENDER
    @Override
    public ApiResponse<LoanPlanResponseDto> createLoanPlan(Long lenderId, LoanPlanRequest request) {

        Lender lender = lenderRepository.findById(lenderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lender not found: " + lenderId)
                );

        LoanPlan loanPlan = new LoanPlan();

        loanPlan.setPlanName(request.getPlanName());
        loanPlan.setAmount(request.getAmount());
        loanPlan.setInterestPerDay(request.getInterestPerDay());
        loanPlan.setPenaltyAmount(request.getPenaltyAmount());
        loanPlan.setPlanDuration(request.getPlanDuration());
        loanPlan.setMaxRadius(request.getMaxRadius());
        loanPlan.setMinCibil(request.getMinCibil());

        // SAFE ENUM CONVERSION
        loanPlan.setStatus(parseStatus(request.getStatus()));

        //  SET RELATION
        loanPlan.setLender(lender);

        LoanPlan saved = repository.save(loanPlan);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Loan plan created successfully",
                mapToResponse(saved)
        );
    }

    @Override
    public ApiResponse<LoanPlanResponseDto> getLoanPlanById(Long id) {

        LoanPlan loanPlan = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Loan Plan not found: " + id)
                );

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Loan Plan Fetched",
                mapToResponse(loanPlan)
        );
    }

    @Override
    public ApiResponse<List<LoanPlanResponseDto>> getAllLoanPlans() {

        List<LoanPlanResponseDto> list = repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "All Loan Plans",
                list
        );
    }

    @Override
    public ApiResponse<LoanPlanResponseDto> updateLoanPlan(Long id, LoanPlanRequest request) {

        LoanPlan plan = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Loan Plan not found: " + id)
                );

        plan.setPlanName(request.getPlanName());
        plan.setAmount(request.getAmount());
        plan.setInterestPerDay(request.getInterestPerDay());
        plan.setPenaltyAmount(request.getPenaltyAmount());
        plan.setPlanDuration(request.getPlanDuration());
        plan.setMaxRadius(request.getMaxRadius());
        plan.setMinCibil(request.getMinCibil());

        //  SAFE ENUM
        plan.setStatus(parseStatus(request.getStatus()));

        LoanPlan updated = repository.save(plan);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Loan Plan Updated",
                mapToResponse(updated)
        );
    }

    @Override
    public ApiResponse<String> deleteLoanPlan(Long id) {

        LoanPlan plan = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Loan Plan not found: " + id)
                );

        repository.delete(plan);

        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                "Loan Plan Deleted",
                "Deleted successfully"
        );
    }

    // MAPPING METHOD
    private LoanPlanResponseDto mapToResponse(LoanPlan entity) {

        LoanPlanResponseDto response = new LoanPlanResponseDto();

        response.setId(entity.getId());
        response.setPlanName(entity.getPlanName());
        response.setLenderId(entity.getLender().getLenderId());
        response.setAmount(entity.getAmount());
        response.setInterestPerDay(entity.getInterestPerDay());
        response.setPenaltyAmount(entity.getPenaltyAmount());
        response.setPlanDuration(entity.getPlanDuration());
        response.setMaxRadius(entity.getMaxRadius());
        response.setMinCibil(entity.getMinCibil());
        response.setStatus(entity.getStatus().name());

        return response;
    }

    //  SAFE ENUM PARSER (NEW)
    private LoanPlanStatus parseStatus(String status) {
        try {
            return LoanPlanStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid LoanPlan status: " + status);
        }
    }
}