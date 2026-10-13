package com.rjdg.mybudget.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rjdg.mybudget.dto.MonthlyReport;
import com.rjdg.mybudget.security.SecurityConfig;
import com.rjdg.mybudget.service.BudgetService;
import com.rjdg.mybudget.service.PdfStatementGenerator;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;
    @MockitoBean
    private PdfStatementGenerator pdfStatementGenerator;

    private final MonthlyReport report = new MonthlyReport("2026-08", new BigDecimal("150.00"), 1, List.of(),
            List.of());

    @Test
    void listsAvailableMonths() throws Exception {
        when(budgetService.getAvailableMonths()).thenReturn(List.of("2026-08", "2026-07"));

        mockMvc.perform(get("/api/reports/months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("2026-08"))
                .andExpect(jsonPath("$[1]").value("2026-07"));
    }

    @Test
    void returnsMonthlyReport() throws Exception {
        when(budgetService.getMonthlyReport(YearMonth.of(2026, 8))).thenReturn(report);

        mockMvc.perform(get("/api/reports/monthly").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-08"))
                .andExpect(jsonPath("$.totalSpent").value(150.00))
                .andExpect(jsonPath("$.transactionCount").value(1));
    }

    @Test
    void rejectsMalformedMonth() throws Exception {
        mockMvc.perform(get("/api/reports/monthly").param("month", "08-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("month must be in YYYY-MM format"));

        verify(budgetService, never()).getMonthlyReport(any());
    }

    @Test
    void missingMonthIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/monthly"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsPdfAsAttachment() throws Exception {
        byte[] pdf = "%PDF-fake".getBytes();
        when(budgetService.getMonthlyReport(YearMonth.of(2026, 8))).thenReturn(report);
        when(pdfStatementGenerator.generate(report)).thenReturn(pdf);

        mockMvc.perform(get("/api/reports/monthly/pdf").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"household-ledger-2026-08.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void pdfGenerationFailureIsServerError() throws Exception {
        when(budgetService.getMonthlyReport(YearMonth.of(2026, 8))).thenReturn(report);
        when(pdfStatementGenerator.generate(report)).thenThrow(new IOException("boom"));

        mockMvc.perform(get("/api/reports/monthly/pdf").param("month", "2026-08"))
                .andExpect(status().isInternalServerError());
    }
}
