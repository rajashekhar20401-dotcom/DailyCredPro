package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final PdfReportService pdfReportService;

    @GetMapping("/lenders/{lenderId}/dashboard.pdf")
    public ResponseEntity<byte[]> downloadLenderDashboardPdf(@PathVariable Long lenderId) {
        byte[] pdfBytes = pdfReportService.generateLenderDashboardPdf(lenderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lender-dashboard-" + lenderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/borrowers/{borrowerId}/analytics.pdf")
    public ResponseEntity<byte[]> downloadBorrowerAnalyticsPdf(@PathVariable Long borrowerId) {
        byte[] pdfBytes = pdfReportService.generateBorrowerAnalyticsPdf(borrowerId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=borrower-analytics-" + borrowerId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}