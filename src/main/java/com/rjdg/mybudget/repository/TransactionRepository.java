package com.rjdg.mybudget.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rjdg.mybudget.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderByDateDescIdDesc();

    boolean existsByCategory_Id(String categoryId);

    List<Transaction> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    List<Transaction> findByAccount_Id(String accountId);

     List<Transaction> findByAccount_IdOrderByDateDesc(String accountId);
}
