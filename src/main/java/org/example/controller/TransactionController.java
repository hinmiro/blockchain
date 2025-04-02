package org.example.controller;

import org.example.dto.TransactionDTO;
import org.example.entity.Block;
import org.example.entity.Transaction;
import org.example.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/v1/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionDTO transaction) {
        System.out.println(transaction.toString());
        try {
            TransactionDTO t = transactionService.processTransaction(transaction);
            System.out.println("Block ready: " + t);
            return ResponseEntity.ok(t);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Transaction failed",
                    "message", e.getMessage()
            ));
        }
    }
}
