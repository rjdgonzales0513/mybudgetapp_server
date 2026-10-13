package com.rjdg.mybudget.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.rjdg.mybudget.dto.MonthlyReport;
import com.rjdg.mybudget.model.Category;
import com.rjdg.mybudget.model.Transaction;

class PdfStatementGeneratorTest {

    private final PdfStatementGenerator generator = new PdfStatementGenerator();

    private static Transaction tx(String description, Category category, String amount) {
        Transaction t = new Transaction();
        t.setDate(LocalDate.of(2026, 8, 15));
        t.setDescription(description);
        t.setType(category == null ? "INCOME" : "EXPENSE");
        t.setCategory(category);
        t.setAmount(new BigDecimal(amount));
        return t;
    }

    private static String extractText(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void rendersSummaryCategoriesAndTransactions() throws Exception {
        Category needs = new Category("needs", "Needs", "home", "#111", new BigDecimal("500"));
        MonthlyReport report = new MonthlyReport("2026-08", new BigDecimal("1234.50"), 2,
                List.of(new MonthlyReport.CategoryTotal("needs", "Needs", "#111", new BigDecimal("1234.50"))),
                List.of(tx("Rent", needs, "1234.50"), tx("Salary", null, "5000.00")));

        byte[] pdf = generator.generate(report);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String text = extractText(pdf);
        assertThat(text)
                .contains("Household Ledger")
                .contains("Monthly Statement - August 2026")
                .contains("Total Spent: $1,234.50")
                .contains("Entries: 2")
                .contains("(100%)")
                .contains("Rent")
                .contains("Salary");
    }

    @Test
    void rendersEmptyMonth() throws Exception {
        MonthlyReport report = new MonthlyReport("2026-01", BigDecimal.ZERO, 0, List.of(), List.of());

        String text = extractText(generator.generate(report));

        assertThat(text)
                .contains("No spending recorded this month.")
                .contains("No entries this month.");
    }

    @Test
    void handlesNonLatinDescriptionsAndManyRowsAcrossPages() throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            transactions.add(tx("Café ☕ 日本 #" + i, null, "1.00"));
        }
        MonthlyReport report = new MonthlyReport("2026-08", BigDecimal.ZERO, transactions.size(), List.of(),
                transactions);

        byte[] pdf = generator.generate(report);

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }
}
