package com.rjdg.mybudget.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rjdg.mybudget.model.Account;

public interface AccountRepository extends JpaRepository<Account, String> {

}
