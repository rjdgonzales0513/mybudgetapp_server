package com.rjdg.mybudget.service;

import com.rjdg.mybudget.dto.MonthlyReport;
import com.rjdg.mybudget.model.Transaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

// Renders a MonthlyReport as a simple one-column PDF statement:
// title, summary, category breakdown, then the full transaction list.
// Uses PDFBox directly (no template engine) since the layout is simple
// enough that manual text positioning is easier to follow than wiring
// up a reporting library for one document type.
@Component
public class PdfStatementGenerator {

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LINE_HEIGHT = 16;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    public byte[] generate(MonthlyReport report) throws IOException {
        try (PDDocument document = new PDDocument(); Cursor cursor = new Cursor(document)) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            cursor.newPage();

            String monthLabel = YearMonth.parse(report.month())
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy"));

            cursor.text("Household Ledger", bold, 20, MARGIN);
            cursor.moveDown(26);
            cursor.text("Monthly Statement - " + monthLabel, regular, 12, MARGIN);
            cursor.moveDown(18);
            cursor.hLine();
            cursor.moveDown(26);

            cursor.text("Total Spent: $" + MONEY.format(report.totalSpent()), bold, 12, MARGIN);
            cursor.moveDown(18);
            cursor.text("Entries: " + report.transactionCount(), regular, 11, MARGIN);
            cursor.moveDown(32);

            cursor.text("By Category", bold, 13, MARGIN);
            cursor.moveDown(20);

            BigDecimal totalSpent = report.totalSpent();
            if (report.byCategory().isEmpty()) {
                cursor.text("No spending recorded this month.", regular, 10, MARGIN);
                cursor.moveDown(LINE_HEIGHT);
            } else {
                for (MonthlyReport.CategoryTotal cat : report.byCategory()) {
                    cursor.ensureSpace(LINE_HEIGHT + 4);
                    int pct = totalSpent.signum() > 0
                            ? cat.getTotal().multiply(BigDecimal.valueOf(100))
                                    .divide(totalSpent, 0, java.math.RoundingMode.HALF_UP).intValue()
                            : 0;
                    cursor.text(cat.getName(), regular, 11, MARGIN);
                    cursor.text("$" + MONEY.format(cat.getTotal()) + "  (" + pct + "%)", regular, 11, MARGIN + 300);
                    cursor.moveDown(LINE_HEIGHT);
                }
            }
            cursor.moveDown(24);

            cursor.ensureSpace(60);
            cursor.text("All Entries", bold, 13, MARGIN);
            cursor.moveDown(20);

            cursor.text("Date", bold, 9, MARGIN);
            cursor.text("Description", bold, 9, MARGIN + 65);
            cursor.text("Category", bold, 9, MARGIN + 300);
            cursor.text("Amount", bold, 9, MARGIN + 430);
            cursor.moveDown(14);
            cursor.hLine();
            cursor.moveDown(12);

            if (report.transactions().isEmpty()) {
                cursor.text("No entries this month.", regular, 10, MARGIN);
            } else {
                for (Transaction tx : report.transactions()) {
                    cursor.ensureSpace(LINE_HEIGHT);
                    cursor.text(String.valueOf(tx.getDate()), regular, 9, MARGIN);
                    cursor.text(truncate(tx.getDescription(), 38), regular, 9, MARGIN + 65);
                    cursor.text(tx.getCategory() != null ? tx.getCategory().getName() : "-", regular, 9, MARGIN + 300);
                    cursor.text("$" + MONEY.format(tx.getAmount()), regular, 9, MARGIN + 430);
                    cursor.moveDown(LINE_HEIGHT);
                }
            }

            cursor.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        // Standard14 fonts only support Latin text; strip anything outside
        // that range rather than let PDFBox throw on an unsupported glyph.
        String safe = s.replaceAll("[^\\x00-\\xFF]", "?");
        return safe.length() > max ? safe.substring(0, max - 1) + "..." : safe;
    }

    // Tracks the current page/content-stream/y-position so callers can
    // just say "write this line" without manually managing page breaks.
    private static class Cursor implements Closeable {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        Cursor(PDDocument document) {
            this.document = document;
        }

        void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN) {
                newPage();
            }
        }

        void text(String s, PDFont font, float size, float x) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, y);
            stream.showText(truncate(s, 200));
            stream.endText();
        }

        void moveDown(float amount) {
            y -= amount;
        }

        void hLine() throws IOException {
            stream.setLineWidth(0.5f);
            stream.moveTo(MARGIN, y);
            stream.lineTo(PAGE_WIDTH - MARGIN, y);
            stream.stroke();
        }

        @Override
        public void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
