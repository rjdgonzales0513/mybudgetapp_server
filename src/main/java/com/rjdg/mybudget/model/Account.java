package com.rjdg.mybudget.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "bank")
    private String bank;

    @Column(name = "icon")
    private String icon;

    @Column(name = "color")
    private String color;

    @Column(precision = 12, scale = 2)
    private BigDecimal balance;

}