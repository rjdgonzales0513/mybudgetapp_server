package com.rjdg.mybudget.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjdg.mybudget.dto.CategoryRequest;
import com.rjdg.mybudget.dto.UpdateCategoryRequest;
import com.rjdg.mybudget.model.Category;
import com.rjdg.mybudget.service.BudgetService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@AllArgsConstructor
public class CategoryController {

    private final BudgetService budgetService;

    @GetMapping
    public List<Category> getCategories() {
        return budgetService.getAllCategories();
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody CategoryRequest request) {
        Category saved = budgetService.createCategory(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saved);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<Category> updateCategoryBudget(@PathVariable String categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {

        Category saved = budgetService.updateCategoryBudget(categoryId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saved);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String categoryId) {
        budgetService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
