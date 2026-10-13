package com.rjdg.mybudget.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.TransactionRequest;
import com.rjdg.mybudget.model.Account;
import com.rjdg.mybudget.model.Category;
import com.rjdg.mybudget.model.Transaction;
import com.rjdg.mybudget.repository.AccountRepository;
import com.rjdg.mybudget.repository.CategoryRepository;
import com.rjdg.mybudget.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;
    private Category needs;

    @BeforeEach
    void setUp() {
        account = new Account("bpi", "BPI Savings", "BPI", "bank", "#000", new BigDecimal("1000.00"));
        needs = new Category("needs", "Needs", "home", "#111", new BigDecimal("500.00"));
    }

    private static TransactionRequest request(String type, String categoryId, String amount) {
        return new TransactionRequest(LocalDate.of(2026, 8, 15), "Groceries", type, categoryId, "bpi",
                new BigDecimal(amount));
    }

    @Test
    void expenseSubtractsFromBalanceAndLinksCategory() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));
        when(categoryRepository.findById("needs")).thenReturn(Optional.of(needs));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction saved = transactionService.addTransaction(request("expense", "needs", "250.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("749.50");
        verify(accountRepository).save(account);
        assertThat(saved.getType()).isEqualTo("EXPENSE");
        assertThat(saved.getCategory()).isSameAs(needs);
        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getAmount()).isEqualByComparingTo("250.50");
        assertThat(saved.getDescription()).isEqualTo("Groceries");
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void expenseEqualToBalanceIsAllowed() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));
        when(categoryRepository.findById("needs")).thenReturn(Optional.of(needs));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.addTransaction(request("EXPENSE", "needs", "1000.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void expenseExceedingBalanceIsRejected() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));
        when(categoryRepository.findById("needs")).thenReturn(Optional.of(needs));

        assertThatThrownBy(() -> transactionService.addTransaction(request("EXPENSE", "needs", "1000.01")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo("Insufficient balance");
                });

        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void expenseWithoutCategoryIsRejected(String categoryId) {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.addTransaction(request("EXPENSE", categoryId, "10.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void expenseWithUnknownCategoryIsNotFound() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));
        when(categoryRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.addTransaction(request("EXPENSE", "ghost", "10.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void incomeAddsToBalanceAndHasNoCategory() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // categoryId supplied on income is ignored
        Transaction saved = transactionService.addTransaction(request("Income", "needs", "5000.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("6000.00");
        verify(accountRepository).save(account);
        assertThat(saved.getType()).isEqualTo("INCOME");
        assertThat(saved.getCategory()).isNull();
        assertThat(saved.getAccount()).isSameAs(account);
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void unknownTypeIsRejected() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.addTransaction(request("TRANSFER", null, "10.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo("type must be EXPENSE or INCOME");
                });

        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void unknownAccountIsNotFound() {
        when(accountRepository.findById("bpi")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.addTransaction(request("INCOME", null, "10.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getAllAccountTransactionReturnsAccountTransactions() {
        Transaction tx = new Transaction();
        when(accountRepository.findById("bpi")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccount_IdOrderByDateDesc("bpi")).thenReturn(List.of(tx));

        assertThat(transactionService.getAllAccountTransaction("bpi")).containsExactly(tx);
    }

    @Test
    void getAllAccountTransactionForUnknownAccountIsNotFound() {
        when(accountRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getAllAccountTransaction("ghost"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(transactionRepository, never()).findByAccount_IdOrderByDateDesc(any());
    }
}
