package com.rjdg.mybudget.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rjdg.mybudget.model.Category;

public interface CategoryRepository extends JpaRepository<Category, String> {

}
