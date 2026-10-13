package com.rjdg.mybudget.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.CategoryRequest;
import com.rjdg.mybudget.dto.UpdateCategoryRequest;
import com.rjdg.mybudget.model.Category;
import com.rjdg.mybudget.security.SecurityConfig;
import com.rjdg.mybudget.service.BudgetService;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

    private final Category needs = new Category("needs", "Needs", "home", "#111", new BigDecimal("500.00"));

    @Test
    void listsCategories() throws Exception {
        when(budgetService.getAllCategories()).thenReturn(List.of(needs));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("needs"))
                .andExpect(jsonPath("$[0].budget").value(500.00));
    }

    @Test
    void createsCategory() throws Exception {
        when(budgetService.createCategory(any(CategoryRequest.class))).thenReturn(needs);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":"needs","name":"Needs","icon":"home","color":"#111","budget":500}
                        """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("needs"));
    }

    @Test
    void rejectsCategoryIdWithUppercaseOrSpaces() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":"My Needs","name":"Needs","icon":"home","color":"#111","budget":500}
                        """))
                .andExpect(status().isBadRequest());

        verify(budgetService, never()).createCategory(any());
    }

    @Test
    void rejectsZeroBudgetOnUpdate() throws Exception {
        mockMvc.perform(put("/api/categories/needs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"budget\":0}"))
                .andExpect(status().isBadRequest());

        verify(budgetService, never()).updateCategoryBudget(any(), any());
    }

    @Test
    void updatesCategoryBudget() throws Exception {
        when(budgetService.updateCategoryBudget(eq("needs"), any(UpdateCategoryRequest.class))).thenReturn(needs);

        mockMvc.perform(put("/api/categories/needs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"budget\":500}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void deletesCategory() throws Exception {
        mockMvc.perform(delete("/api/categories/leisure"))
                .andExpect(status().isNoContent());

        verify(budgetService).deleteCategory("leisure");
    }

    @Test
    void deleteCategoryInUseIsConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "in use"))
                .when(budgetService).deleteCategory("needs");

        mockMvc.perform(delete("/api/categories/needs"))
                .andExpect(status().isConflict());
    }
}
