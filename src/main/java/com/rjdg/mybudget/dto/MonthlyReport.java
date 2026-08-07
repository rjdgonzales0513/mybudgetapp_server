package com.rjdg.mybudget.dto;

import java.math.BigDecimal;
import java.util.List;

import com.rjdg.mybudget.model.Transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public record MonthlyReport(String month, BigDecimal totalSpent, int transactionCount, List<CategoryTotal> byCategory,
        List<Transaction> transactions) {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryTotal {
        private String categoryId;
        private String name;
        private String color;
        private BigDecimal total;
    }
}
