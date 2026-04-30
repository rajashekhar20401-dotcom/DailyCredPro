package com.unqiuehire.kashflow.serviceimpl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import com.unqiuehire.kashflow.dto.responsedto.BorrowerAnalyticsSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.LenderDashboardSummaryResponseDto;
import com.unqiuehire.kashflow.dto.responsedto.RepaymentResponseDTO;
import com.unqiuehire.kashflow.service.BorrowerAnalyticsService;
import com.unqiuehire.kashflow.service.LenderAnalyticsService;
import com.unqiuehire.kashflow.service.PdfReportService;
import com.unqiuehire.kashflow.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfReportServiceImpl implements PdfReportService {

    private final LenderAnalyticsService lenderAnalyticsService;
    private final BorrowerAnalyticsService borrowerAnalyticsService;
    private final RepaymentService repaymentService;

    @Override
    public byte[] generateLenderDashboardPdf(Long lenderId) {
        try {
            LenderDashboardSummaryResponseDto summary = lenderAnalyticsService.getDashboardSummary(lenderId);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();
            document.add(new Paragraph("Lender Dashboard Report"));
            document.add(new Paragraph("Generated At: " + LocalDateTime.now()));
            document.add(new Paragraph("Lender ID: " + lenderId));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.addCell("Total Loans");
            table.addCell(String.valueOf(summary.getTotalLoans()));
            table.addCell("Active Loans");
            table.addCell(String.valueOf(summary.getActiveLoans()));
            table.addCell("Closed Loans");
            table.addCell(String.valueOf(summary.getClosedLoans()));
            table.addCell("Total Principal Disbursed");
            table.addCell(String.valueOf(summary.getTotalPrincipalDisbursed()));
            table.addCell("Total Collected");
            table.addCell(String.valueOf(summary.getTotalCollected()));
            table.addCell("Total Outstanding");
            table.addCell(String.valueOf(summary.getTotalOutstanding()));
            table.addCell("Total Penalty Collected");
            table.addCell(String.valueOf(summary.getTotalPenaltyCollected()));
            table.addCell("Projected Profit");
            table.addCell(String.valueOf(summary.getProjectedProfit()));
            table.addCell("Current Outstanding Exposure");
            table.addCell(String.valueOf(summary.getCurrentOutstandingExposure()));
            table.addCell("Recoverable Amount");
            table.addCell(String.valueOf(summary.getRecoverableAmount()));
            table.addCell("Capital Ready For Reuse");
            table.addCell(String.valueOf(summary.getCapitalReadyForReuse()));
            table.addCell("Paid Today");
            table.addCell(String.valueOf(summary.getPaidToday()));
            table.addCell("Missed Today");
            table.addCell(String.valueOf(summary.getMissedToday()));
            table.addCell("Partial Today");
            table.addCell(String.valueOf(summary.getPartialToday()));
            table.addCell("Advance Today");
            table.addCell(String.valueOf(summary.getAdvanceToday()));
            table.addCell("Overdue Borrowers");
            table.addCell(String.valueOf(summary.getOverdueBorrowers()));

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate lender dashboard PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateBorrowerAnalyticsPdf(Long borrowerId) {
        try {
            ApiResponse<BorrowerAnalyticsSummaryResponseDto> response = borrowerAnalyticsService.getBorrowerSummary(borrowerId);
            if (response.getData() == null) {
                throw new RuntimeException("Borrower analytics not found");
            }

            BorrowerAnalyticsSummaryResponseDto summary = response.getData();
            List<RepaymentResponseDTO> repayments = repaymentService.getByBorrower(borrowerId);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);

            document.open();
            document.add(new Paragraph("Borrower Analytics Report"));
            document.add(new Paragraph("Generated At: " + LocalDateTime.now()));
            document.add(new Paragraph("Borrower: " + summary.getBorrowerName() + " (ID: " + summary.getBorrowerId() + ")"));
            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.addCell("Internal Credit Score");
            summaryTable.addCell(String.valueOf(summary.getInternalCreditScore()));
            summaryTable.addCell("Risk Score");
            summaryTable.addCell(String.valueOf(summary.getRiskScore()));
            summaryTable.addCell("Risk Category");
            summaryTable.addCell(summary.getRiskCategory());
            summaryTable.addCell("Eligibility Tier");
            summaryTable.addCell(summary.getEligibilityTier());
            summaryTable.addCell("Eligibility Status");
            summaryTable.addCell(summary.getEligibilityStatus());
            summaryTable.addCell("Max Eligible Loan Amount");
            summaryTable.addCell(String.valueOf(summary.getMaxEligibleLoanAmount()));
            summaryTable.addCell("Total Loans Taken");
            summaryTable.addCell(String.valueOf(summary.getTotalLoansTaken()));
            summaryTable.addCell("Active Loans");
            summaryTable.addCell(String.valueOf(summary.getActiveLoans()));
            summaryTable.addCell("Closed Loans");
            summaryTable.addCell(String.valueOf(summary.getClosedLoans()));
            summaryTable.addCell("Defaulted Loan Count");
            summaryTable.addCell(String.valueOf(summary.getDefaultedLoanCount()));
            summaryTable.addCell("Total Missed Days");
            summaryTable.addCell(String.valueOf(summary.getTotalMissedDays()));
            summaryTable.addCell("Total Partial Payments");
            summaryTable.addCell(String.valueOf(summary.getTotalPartialPayments()));
            summaryTable.addCell("Total Advance Payments");
            summaryTable.addCell(String.valueOf(summary.getTotalAdvancePayments()));
            summaryTable.addCell("Total Late Payments");
            summaryTable.addCell(String.valueOf(summary.getTotalLatePayments()));
            summaryTable.addCell("Average Missed Days Per Loan");
            summaryTable.addCell(String.valueOf(summary.getAverageMissedDaysPerLoan()));
            summaryTable.addCell("Current Outstanding Amount");
            summaryTable.addCell(String.valueOf(summary.getCurrentOutstandingAmount()));

            document.add(summaryTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Repayment History"));
            document.add(new Paragraph(" "));

            PdfPTable repaymentTable = new PdfPTable(7);
            repaymentTable.addCell("Repayment ID");
            repaymentTable.addCell("Payment Date");
            repaymentTable.addCell("Amount Paid");
            repaymentTable.addCell("Mode");
            repaymentTable.addCell("Status");
            repaymentTable.addCell("Missed Days");
            repaymentTable.addCell("Balance Amount");

            for (RepaymentResponseDTO repayment : repayments) {
                repaymentTable.addCell(String.valueOf(repayment.getId()));
                repaymentTable.addCell(String.valueOf(repayment.getPaymentDate()));
                repaymentTable.addCell(String.valueOf(repayment.getAmountPaid()));
                repaymentTable.addCell(String.valueOf(repayment.getPaymentMode()));
                repaymentTable.addCell(String.valueOf(repayment.getPaymentStatus()));
                repaymentTable.addCell(String.valueOf(repayment.getMissedDays()));
                repaymentTable.addCell(String.valueOf(repayment.getBalanceAmount()));
            }

            document.add(repaymentTable);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate borrower analytics PDF: " + e.getMessage(), e);
        }
    }
}