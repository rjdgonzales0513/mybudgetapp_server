package com.rjdg.mybudget.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.rjdg.mybudget.dto.AccountRequest;
import com.rjdg.mybudget.model.Account;
import com.rjdg.mybudget.repository.AccountRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;

    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountInformation(String accountId) {
        return accountRepository.findById(accountId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No account with id '" + accountId + "' does not exist"));

    }

    public Account addAccount(AccountRequest request) {

        String accountId = request.id();
        if (accountRepository.findById(accountId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account with id '" + accountId + "' already exists");
        }

        Account account = new Account(accountId, request.name(), request.bank(), request.icon(), request.color(),
                request.balance());

        return accountRepository.save(account);
    }
}
