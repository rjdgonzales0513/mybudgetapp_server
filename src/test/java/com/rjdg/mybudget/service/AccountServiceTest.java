package com.rjdg.mybudget.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.AccountRequest;
import com.rjdg.mybudget.model.Account;
import com.rjdg.mybudget.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private final AccountRequest request = new AccountRequest("bpi", "BPI Savings", "BPI", "bank", "#000",
            new BigDecimal("100.00"));

    @Test
    void addAccountSavesNewAccount() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account saved = accountService.addAccount(request);

        assertThat(saved.getId()).isEqualTo("bpi");
        assertThat(saved.getName()).isEqualTo("BPI Savings");
        assertThat(saved.getBank()).isEqualTo("BPI");
        assertThat(saved.getIcon()).isEqualTo("bank");
        assertThat(saved.getColor()).isEqualTo("#000");
        assertThat(saved.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void addAccountWithDuplicateIdIsConflict() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(new Account()));

        assertThatThrownBy(() -> accountService.addAccount(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAccountInformationReturnsAccount() {
        Account account = new Account();
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));

        assertThat(accountService.getAccountInformation("bpi")).isSameAs(account);
    }

    @Test
    void getAccountInformationForUnknownIdIsNotFound() {
        when(accountRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountInformation("ghost"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
