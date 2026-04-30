package com.unqiuehire.kashflow.controller;
import com.unqiuehire.kashflow.dto.requestdto.LenderRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.LenderResponseDto;
import com.unqiuehire.kashflow.service.LenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lenders")
public class LenderController {

    @Autowired
    private LenderService lenderService;

    @PostMapping
    public ApiResponse<LenderResponseDto> createLender(@RequestBody LenderRequestDto lenderRequestDto) {
        return lenderService.createLender(lenderRequestDto);
    }

    @GetMapping("/{lenderId}")
    public ApiResponse<LenderResponseDto> getLenderById(@PathVariable Long lenderId) {
        return lenderService.getLenderById(lenderId);
    }

    @GetMapping
    public ApiResponse<List<LenderResponseDto>> getAllLenders() {
        return lenderService.getAllLenders();
    }

    @PutMapping("/{lenderId}")
    public ApiResponse<LenderResponseDto> updateLender(@PathVariable Long lenderId,
                                                       @RequestBody LenderRequestDto lenderRequestDto) {
        return lenderService.updateLender(lenderId, lenderRequestDto);
    }

    @DeleteMapping("/{lenderId}")
    public ApiResponse<String> deleteLender(@PathVariable Long lenderId) {
        return lenderService.deleteLender(lenderId);
    }
}