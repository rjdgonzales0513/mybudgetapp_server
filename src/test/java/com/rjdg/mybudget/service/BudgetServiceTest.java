package com.rjdg.mybudget.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.CategoryRequest;
import com.rjdg.mybudget.dto.MonthlyReport;
import com.rjdg.mybudget.dto.UpdateCategoryRequest;
import com.rjdg.mybudget.model.Category;
import com.rjdg.mybudget.model.Transaction;
import com.rjdg.mybudget.repository.CategoryRepository;
import com.rjdg.mybudget.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    private final Category needs = new Category("needs", "Needs", "home", "#111", new BigDecimal("500"));
    private final Category leisure = new Category("leisure", "Leisure", "game", "#222", new BigDecimal("200"));

    private static Transaction tx(String date, String type, Category category, String amount) {
        Transaction t = new Transaction();
        t.setDate(LocalDate.parse(date));
        t.setType(type);
        t.setCategory(category);
        t.setAmount(new BigDecimal(amount));
        t.setDescription("tx");
        return t;
    }

    // --- categories ---

    @Test
    void createCategorySavesNewCategory() {
        CategoryRequest request = new CategoryRequest("travel", "Travel", "plane", "#333", new BigDecimal("150"));
        when(categoryRepository.existsById("travel")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category saved = budgetService.createCategory(request);

        assertThat(saved.getId()).isEqualTo("travel");
        assertThat(saved.getName()).isEqualTo("Travel");
        assertThat(saved.getBudget()).isEqualByComparingTo("150");
    }

    @Test
    void createCategoryWithDuplicateIdIsConflict() {
        CategoryRequest request = new CategoryRequest("needs", "Needs", "home", "#111", new BigDecimal("1"));
        when(categoryRepository.existsById("needs")).thenReturn(true);

        assertThatThrownBy(() -> budgetService.createCategory(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategoryBudgetChangesOnlyBudget() {
        when(categoryRepository.findById("needs")).thenReturn(Optional.of(needs));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category saved = budgetService.updateCategoryBudget("needs", new UpdateCategoryRequest(new BigDecimal("750")));

        assertThat(saved.getBudget()).isEqualByComparingTo("750");
        assertThat(saved.getName()).isEqualTo("Needs");
    }

    @Test
    void updateUnknownCategoryIsNotFound() {
        when(categoryRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateCategoryBudget("ghost",
                new UpdateCategoryRequest(BigDecimal.ONE)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteCategoryWithoutTransactionsDeletes() {
        when(categoryRepository.existsById("leisure")).thenReturn(true);
        when(transactionRepository.existsByCategory_Id("leisure")).thenReturn(false);

        budgetService.deleteCategory("leisure");

        verify(categoryRepository).deleteById("leisure");
    }

    @Test
    void deleteUnknownCategoryIsNotFound() {
        when(categoryRepository.existsById("ghost")).thenReturn(false);

        assertThatThrownBy(() -> budgetService.deleteCategory("ghost"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void deleteCategoryWithTransactionsIsConflict() {
        when(categoryRepository.existsById("needs")).thenReturn(true);
        when(transactionRepository.existsByCategory_Id("needs")).thenReturn(true);

        assertThatThrownBy(() -> budgetService.deleteCategory("needs"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(categoryRepository, never()).deleteById(any());
    }

    // --- reports ---

    @Test
    void availableMonthsAreDistinctAndNewestFirst() {
        when(transactionRepository.findAll()).thenReturn(List.of(
                tx("2026-06-03", "EXPENSE", needs, "1"),
                tx("2026-08-20", "EXPENSE", needs, "1"),
                tx("2026-06-28", "INCOME", null, "1"),
                tx("2025-12-31", "EXPENSE", needs, "1")));

        assertThat(budgetService.getAvailableMonths()).containsExactly("2026-08", "2026-06", "2025-12");
    }

    @Test
    void monthlyReportQueriesWholeMonth() {
        YearMonth feb = YearMonth.of(2028, 2); // leap year
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any())).thenReturn(List.of());

        budgetService.getMonthlyReport(feb);

        verify(transactionRepository).findByDateBetweenOrderByDateDesc(
                LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29));
    }

    @Test
    void monthlyReportTotalsExpensesOnlyAndGroupsByCategory() {
        List<Transaction> transactions = List.of(
                tx("2026-08-25", "EXPENSE", leisure, "30.00"),
                tx("2026-08-20", "EXPENSE", needs, "100.00"),
                tx("2026-08-15", "INCOME", null, "5000.00"),
                tx("2026-08-10", "EXPENSE", needs, "50.25"),
                tx("2026-08-05", "expense", leisure, "10.00"));
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any())).thenReturn(transactions);

        MonthlyReport report = budgetService.getMonthlyReport(YearMonth.of(2026, 8));

        assertThat(report.month()).isEqualTo("2026-08");
        assertThat(report.totalSpent()).isEqualByComparingTo("190.25");
        assertThat(report.transactionCount()).isEqualTo(5);
        assertThat(report.transactions()).isEqualTo(transactions);

        // sorted by total descending; income (no category) is excluded
        assertThat(report.byCategory()).extracting(MonthlyReport.CategoryTotal::getCategoryId)
                .containsExactly("needs", "leisure");
        assertThat(report.byCategory().get(0).getTotal()).isEqualByComparingTo("150.25");
        assertThat(report.byCategory().get(0).getName()).isEqualTo("Needs");
        assertThat(report.byCategory().get(0).getColor()).isEqualTo("#111");
        assertThat(report.byCategory().get(1).getTotal()).isEqualByComparingTo("40.00");
    }

    @Test
    void monthlyReportForEmptyMonth() {
        when(transactionRepository.findByDateBetweenOrderByDateDesc(any(), any())).thenReturn(List.of());

        MonthlyReport report = budgetService.getMonthlyReport(YearMonth.of(2026, 1));

        assertThat(report.totalSpent()).isEqualByComparingTo("0");
        assertThat(report.transactionCount()).isZero();
        assertThat(report.byCategory()).isEmpty();
        assertThat(report.transactions()).isEmpty();
    }
}
