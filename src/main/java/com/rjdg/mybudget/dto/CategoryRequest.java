package com.rjdg.mybudget.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CategoryRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$", message = "id must be lowercase letters, numbers, and hyphens only") String id,
        @NotNull String name, @NotBlank String icon, @NotBlank String color,
        @NotNull @DecimalMin(value = "0.01") BigDecimal budget) {

}
