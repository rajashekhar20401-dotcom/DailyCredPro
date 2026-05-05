package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.responsedto.RewardEventResponseDto;
import com.unqiuehire.kashflow.entity.RewardEvent;
import com.unqiuehire.kashflow.repository.RewardEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reward-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RewardEventController {

    private final RewardEventRepository rewardEventRepository;

    @GetMapping("/loan/{loanId}")
    public List<RewardEventResponseDto> getByLoan(@PathVariable Long loanId) {
        return rewardEventRepository.findByLoanIdOrderByEventDateDescCreatedAtDesc(loanId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private RewardEventResponseDto mapToDto(RewardEvent event) {
        RewardEventResponseDto dto = new RewardEventResponseDto();
        dto.setId(event.getId());
        dto.setLoanId(event.getLoanId());
        dto.setRepaymentId(event.getRepaymentId());
        dto.setBorrowerId(event.getBorrowerId());
        dto.setLenderId(event.getLenderId());
        dto.setEventDate(event.getEventDate());
        dto.setReasonType(event.getReasonType() == null ? null : event.getReasonType().name());
        dto.setRewardPercent(event.getRewardPercent());
        dto.setBaseAmount(event.getBaseAmount() == null ? 0.0 : event.getBaseAmount().doubleValue());
        dto.setRewardAmount(event.getRewardAmount() == null ? 0.0 : event.getRewardAmount().doubleValue());
        dto.setNote(event.getNote());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}