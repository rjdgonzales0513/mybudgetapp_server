package com.rjdg.mybudget.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.rjdg.mybudget.dto.TransactionRequest;
import com.rjdg.mybudget.model.Transaction;
import com.rjdg.mybudget.security.SecurityConfig;
import com.rjdg.mybudget.service.TransactionService;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    private static Transaction savedTransaction() {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setDate(LocalDate.of(2026, 8, 15));
        tx.setDescription("Groceries");
        tx.setType("EXPENSE");
        tx.setAmount(new BigDecimal("25.00"));
        return tx;
    }

    @Test
    void listsTransactions() throws Exception {
        when(transactionService.getAllTransactions()).thenReturn(List.of(savedTransaction()));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].date").value("2026-08-15"))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    @Test
    void createsTransaction() throws Exception {
        when(transactionService.addTransaction(any(TransactionRequest.class))).thenReturn(savedTransaction());

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"date":"2026-08-15","description":"Groceries","type":"expense",
                         "categoryId":"needs","accountId":"bpi","amount":25.00}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void rejectsInvalidTransactionBody() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"date":null,"description":"","type":"EXPENSE","accountId":"bpi","amount":0}
                        """))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).addTransaction(any());
    }

    @Test
    void surfacesServiceErrorStatusAndReason() throws Exception {
        when(transactionService.addTransaction(any(TransactionRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance"));

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"date":"2026-08-15","description":"TV","type":"EXPENSE",
                         "categoryId":"leisure","accountId":"bpi","amount":99999}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Insufficient balance"));
    }

    @Test
    void listsAccountTransactions() throws Exception {
        when(transactionService.getAllAccountTransaction("bpi")).thenReturn(List.of(savedTransaction()));

        mockMvc.perform(get("/api/transactions/bpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
