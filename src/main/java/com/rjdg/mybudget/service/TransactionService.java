package com.rjdg.mybudget.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.TransactionRequest;
import com.rjdg.mybudget.model.Account;
import com.rjdg.mybudget.model.Category;
import com.rjdg.mybudget.model.Transaction;
import com.rjdg.mybudget.repository.AccountRepository;
import com.rjdg.mybudget.repository.CategoryRepository;
import com.rjdg.mybudget.repository.TransactionRepository;

@Service
public class TransactionService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private static final String EXPENSE = "EXPENSE";
    private static final String INCOME = "INCOME";

    public TransactionService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByDateDescIdDesc();
    }

    @Transactional
    public Transaction addTransaction(TransactionRequest request) {

        Account account = validateAccount(request.accountId());
        BigDecimal balance = account.getBalance();

        Transaction transaction = new Transaction();
        transaction.setDate(request.date());
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());

        if (request.type().equalsIgnoreCase(EXPENSE)) {
            if (request.categoryId() == null || request.categoryId().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "categoryId is required for an expense");
            }
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No category with id " + request.categoryId()));
            if (request.amount().compareTo(balance) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Insufficient balance");
            }

            account.setBalance(balance.subtract(request.amount()));
            accountRepository.save(account);

            transaction.setType(EXPENSE);
            transaction.setCategory(category);
            transaction.setAccount(account);
            return transactionRepository.save(transaction);
        } else if (request.type().equalsIgnoreCase(INCOME)) {
            account.setBalance(balance.add(request.amount()));
            accountRepository.save(account);

            transaction.setType(INCOME);
            transaction.setAccount(account);
            return transactionRepository.save(transaction);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "type must be EXPENSE or INCOME");
        }
    }

    public List<Transaction> getAllAccountTransaction(String accountId) {
        validateAccount(accountId);
        return transactionRepository.findByAccount_IdOrderByDateDesc(accountId);
    }

    private Account validateAccount(String accountId) {
        return accountRepository.findById(accountId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No account with id " + accountId));
    }

}
