package com.rjdg.mybudget.controller;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.MonthlyReport;
import com.rjdg.mybudget.service.BudgetService;
import com.rjdg.mybudget.service.PdfStatementGenerator;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@AllArgsConstructor
public class ReportController {

    private final BudgetService budgetService;
    private final PdfStatementGenerator pdfStatementGenerator;

   @GetMapping("/months")
    public List<String> getAvailableMonths() {
        return budgetService.getAvailableMonths();
    }

    // e.g. /api/reports/monthly?month=2026-08
    @GetMapping("/monthly")
    public MonthlyReport getMonthlyReport(@RequestParam String month) {
        return budgetService.getMonthlyReport(YearMonth.parse(month));
    }

    // e.g. /api/reports/monthly/pdf?month=2026-08
    @GetMapping(value = "/monthly/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getMonthlyReportPdf(@RequestParam String month) {
        MonthlyReport report = budgetService.getMonthlyReport(YearMonth.parse(month));
        byte[] pdf;
        try {
            pdf = pdfStatementGenerator.generate(report);
        } catch (IOException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate PDF statement", e);
        }
        String filename = "household-ledger-" + month + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
