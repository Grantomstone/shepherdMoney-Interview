package com.shepherdmoney.interviewproject.model;

import java.time.LocalDate;
import java.util.Comparator;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Embeddable
@Setter
@RequiredArgsConstructor
public class BalanceHistory implements Comparator<BalanceHistory>, Comparable<BalanceHistory> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    
    private LocalDate date;

    private double balance;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "card_id")
    private CreditCard card;

    public BalanceHistory(LocalDate date, double balance, CreditCard card) {
        this.date = date;
        this.balance = balance;
        this.card = card;
    }

    @Override
    public int compare( BalanceHistory balance1, BalanceHistory balance2) {
        return balance1.getDate().compareTo(balance2.getDate());
    }

    @Override
    public int compareTo( BalanceHistory other ) {
        if (Double.isNaN(this.getBalance())) {
            return 1;
        } else if (Double.isNaN(other.getBalance())) {
            return -1;
        }
        return Double.compare(this.getBalance(), other.getBalance());
    }

    @Override
    public String toString() {
        return "BalanceHistory: { id: " + id + ", date: " + date + ", balance: " + balance + "}";
    }
}
