package com.ncusoft.myapplication7.controller;

import com.ncusoft.myapplication7.entity.Transaction;
import com.ncusoft.myapplication7.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @GetMapping("/{userId}")
    public List<Transaction> getTransactions(@PathVariable int userId) {
        return transactionService.getTransactionsByUserId(userId);
    }

    @PostMapping
    public Map<String, Object> addTransaction(@RequestBody Transaction transaction) {
        Map<String, Object> result = new HashMap<>();
        boolean ok = transactionService.addTransaction(transaction);
        result.put("success", ok);
        result.put("message", ok ? "添加交易记录成功" : "添加交易记录失败: 数据库插入错误");
        return result;
    }

    @GetMapping("/search")
    public List<Transaction> searchTransactions(
            @RequestParam int userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) BigDecimal amount
    ) {
        return transactionService.searchTransactions(userId, type, note, year, month, day, amount);
    }
}
