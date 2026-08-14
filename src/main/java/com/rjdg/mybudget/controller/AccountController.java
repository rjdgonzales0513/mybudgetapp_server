package com.rjdg.mybudget.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjdg.mybudget.dto.AccountRequest;
import com.rjdg.mybudget.model.Account;
import com.rjdg.mybudget.model.Transaction;
import com.rjdg.mybudget.service.AccountService;
import com.rjdg.mybudget.service.BudgetService;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/accounts")
@AllArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@GetMapping
	public List<Account> getAllAccounts() {
		return accountService.getAllAccounts();
	}

	@PostMapping
	public ResponseEntity<Account> addAccount(@RequestBody AccountRequest accountRequest) {
		Account saved = accountService.addAccount(accountRequest);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(saved);
	}

	@GetMapping("/{accountId}")
	public Account getAccountInformation(@PathVariable String accountId) {

		return accountService.getAccountInformation(accountId);
	}

}
