package com.ncusoft.myapplication7.service;

import com.ncusoft.myapplication7.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {
    List<Transaction> getTransactionsByUserId(int userId);
    boolean addTransaction(Transaction transaction);
    List<Transaction> searchTransactions(int userId, Integer type, String note, Integer year, Integer month, Integer day, BigDecimal amount);
}
