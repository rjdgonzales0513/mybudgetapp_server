package com.rjdg.mybudget.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.AccountRequest;
import com.rjdg.mybudget.model.Account;
import com.rjdg.mybudget.security.SecurityConfig;
import com.rjdg.mybudget.service.AccountService;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private final Account account = new Account("bpi", "BPI Savings", "BPI", "bank", "#000",
            new BigDecimal("100.00"));

    @Test
    void listsAccounts() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(List.of(account));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("bpi"))
                .andExpect(jsonPath("$[0].balance").value(100.00));
    }

    @Test
    void createsAccount() throws Exception {
        when(accountService.addAccount(any(AccountRequest.class))).thenReturn(account);

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":"bpi","name":"BPI Savings","bank":"BPI","icon":"bank","color":"#000","balance":100}
                        """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("bpi"));
    }

    @Test
    void rejectsNegativeOpeningBalance() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":"bpi","name":"BPI Savings","bank":"BPI","icon":"bank","color":"#000","balance":-1}
                        """))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).addAccount(any());
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        when(accountService.getAccountInformation("ghost"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "nope"));

        mockMvc.perform(get("/api/accounts/ghost"))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsAllowsViteDevServer() throws Exception {
        mockMvc.perform(options("/api/accounts")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsRejectsOtherOrigins() throws Exception {
        mockMvc.perform(options("/api/accounts")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
