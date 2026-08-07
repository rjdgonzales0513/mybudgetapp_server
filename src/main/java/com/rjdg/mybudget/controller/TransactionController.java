package com.rjdg.mybudget.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjdg.mybudget.dto.TransactionRequest;
import com.rjdg.mybudget.model.Transaction;
import com.rjdg.mybudget.service.BudgetService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api/transactions")
@AllArgsConstructor
public class TransactionController {

	private final BudgetService budgetService;

	@GetMapping
	public List<Transaction> getTransactions() {
		return budgetService.getAllTransactions();
	}

	@PostMapping
	public ResponseEntity<Transaction> addTransaction(@Valid @RequestBody TransactionRequest request) {
		Transaction saved = budgetService.addTransaction(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

}
