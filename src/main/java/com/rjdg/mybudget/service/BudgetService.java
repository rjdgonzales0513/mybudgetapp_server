package com.rjdg.mybudget.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.CategoryRequest;
import com.rjdg.mybudget.dto.MonthlyReport;
import com.rjdg.mybudget.dto.UpdateCategoryRequest;
import com.rjdg.mybudget.model.Category;
import com.rjdg.mybudget.model.Transaction;
import com.rjdg.mybudget.repository.CategoryRepository;
import com.rjdg.mybudget.repository.TransactionRepository;

@Service
public class BudgetService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(CategoryRequest request) {

        if (isCategoryPresent(request.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A category with id '" + request.id() + "' already exists");
        }

        Category category = new Category(request.id(), request.name(), request.icon(), request.color(),
                request.budget());

        categoryRepository.save(category);

        return categoryRepository.save(category);
    }

    public Category updateCategoryBudget(String categoryId, UpdateCategoryRequest request) {

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No category with id '" + categoryId + "' does not exist"));

        category.setBudget(request.budget());

        return categoryRepository.save(category);

    }

    public void deleteCategory(String categoryId) {
        if (!isCategoryPresent(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No category with id '" + categoryId + "' does not exist");
        }

        if (transactionRepository.existsByCategory_Id(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a category that has transactions. Remove or reassign its transactions first.");
        }

        categoryRepository.deleteById(categoryId);
    }

    public List<String> getAvailableMonths() {
        return transactionRepository.findAll().stream()
                .map(t -> YearMonth.from(t.getDate()))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .map(YearMonth::toString)
                .collect(Collectors.toList());
    }

    public MonthlyReport getMonthlyReport(YearMonth month) {
        List<Transaction> transactions = transactionRepository.findByDateBetweenOrderByDateDesc(
                month.atDay(1), month.atEndOfMonth());

        Map<String, MonthlyReport.CategoryTotal> totalsByCategory = new LinkedHashMap<>();
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            if (tx.getType().equalsIgnoreCase("EXPENSE")) {
                totalSpent = totalSpent.add(tx.getAmount());
            }
            
            Category cat = tx.getCategory();
            if (cat == null)
                continue;

            MonthlyReport.CategoryTotal existing = totalsByCategory.get(cat.getId());
            if (existing == null) {
                totalsByCategory.put(cat.getId(),
                        new MonthlyReport.CategoryTotal(cat.getId(), cat.getName(), cat.getColor(), tx.getAmount()));
            } else {
                existing.setTotal(existing.getTotal().add(tx.getAmount()));
            }
        }

        List<MonthlyReport.CategoryTotal> byCategory = new ArrayList<>(totalsByCategory.values());
        byCategory.sort((a, b) -> b.getTotal().compareTo(a.getTotal()));

        MonthlyReport report = new MonthlyReport(month.toString(), totalSpent, transactions.size(), byCategory,
                transactions);

        return report;
    }

    private boolean isCategoryPresent(String id) {
        return categoryRepository.existsById(id) ? true : false;

    }
}
