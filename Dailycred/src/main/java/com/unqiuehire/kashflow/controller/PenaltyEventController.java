package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.responsedto.PenaltyEventResponseDto;
import com.unqiuehire.kashflow.entity.PenaltyEvent;
import com.unqiuehire.kashflow.repository.PenaltyEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/penalty-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PenaltyEventController {

    private final PenaltyEventRepository penaltyEventRepository;

    @GetMapping("/loan/{loanId}")
    public List<PenaltyEventResponseDto> getByLoan(@PathVariable Long loanId) {
        return penaltyEventRepository.findByLoanIdOrderByEventDateDescCreatedAtDesc(loanId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private PenaltyEventResponseDto mapToDto(PenaltyEvent event) {
        PenaltyEventResponseDto dto = new PenaltyEventResponseDto();
        dto.setId(event.getId());
        dto.setLoanId(event.getLoanId());
        dto.setRepaymentId(event.getRepaymentId());
        dto.setBorrowerId(event.getBorrowerId());
        dto.setLenderId(event.getLenderId());
        dto.setEventDate(event.getEventDate());
        dto.setReasonType(event.getReasonType() == null ? null : event.getReasonType().name());
        dto.setTriggerCount(event.getTriggerCount());
        dto.setThresholdValue(event.getThresholdValue());
        dto.setDailyInterestAmount(event.getDailyInterestAmount() == null ? 0.0 : event.getDailyInterestAmount().doubleValue());
        dto.setPenaltyPercent(event.getPenaltyPercent());
        dto.setAmount(event.getAmount() == null ? 0.0 : event.getAmount().doubleValue());
        dto.setNote(event.getNote());
        dto.setResolved(event.getResolved());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }
}