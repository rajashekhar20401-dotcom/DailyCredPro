package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.dto.requestdto.BorrowerRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerResponseDto;
import com.unqiuehire.kashflow.service.BorrowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrowers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BorrowerController {

    private final BorrowerService borrowerService;

    @PostMapping
    public ApiResponse<BorrowerResponseDto> createBorrower(@RequestBody BorrowerRequestDto borrowerRequestDto) {
        return borrowerService.createBorrower(borrowerRequestDto);
    }

    @GetMapping("/{borrowerId}")
    public ApiResponse<BorrowerResponseDto> getBorrowerById(@PathVariable Long borrowerId) {
        return borrowerService.getBorrowerById(borrowerId);
    }

    @GetMapping
    public ApiResponse<List<BorrowerResponseDto>> getAllBorrowers() {
        return borrowerService.getAllBorrowers();
    }

    @PutMapping("/{borrowerId}")
    public ApiResponse<BorrowerResponseDto> updateBorrower(
            @PathVariable Long borrowerId,
            @RequestBody BorrowerRequestDto borrowerRequestDto
    ) {
        return borrowerService.updateBorrower(borrowerId, borrowerRequestDto);
    }

    @DeleteMapping("/{borrowerId}")
    public ApiResponse<String> deleteBorrower(@PathVariable Long borrowerId) {
        return borrowerService.deleteBorrower(borrowerId);
    }
}