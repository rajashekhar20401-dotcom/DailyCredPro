package com.unqiuehire.kashflow.dto.requestdto;

import com.unqiuehire.kashflow.constant.ApplicationStatus;
import lombok.Data;

@Data
public class LoanApplicationApprovalRequestDto {
    private ApplicationStatus applicationStatus;
    private String remarks;
}
