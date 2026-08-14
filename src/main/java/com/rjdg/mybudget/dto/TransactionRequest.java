package com.rjdg.mybudget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(@NotNull LocalDate date, @NotBlank String description, @NotBlank String type,
                String categoryId, @NotBlank String accountId,
                @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {

}
