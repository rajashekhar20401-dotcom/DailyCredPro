package com.unqiuehire.kashflow.service;

public interface PdfReportService {
    byte[] generateLenderDashboardPdf(Long lenderId);
    byte[] generateBorrowerAnalyticsPdf(Long borrowerId);
}