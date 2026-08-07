package com.rjdg.mybudget.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryRequest(
                @NotNull @DecimalMin(value = "0.01") BigDecimal budget) {

}
