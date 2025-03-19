package org.example.controller;

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
    public ResponseEntity<?> createTransaction(@RequestBody Transaction transaction) {
        System.out.println(transaction.toString());
        try {
            Block block = transactionService.processTransaction(transaction);
            return ResponseEntity.ok(Map.of(
                    "message", "Transaction processed successfully",
                    "blockhash", block.getHash(),
                    "transactionId", transaction.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Transaction failed",
                    "message", e.getMessage()
            ));
        }
    }
}
