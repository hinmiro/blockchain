package org.example.controller;

import org.example.dto.TransactionDTO;
import org.example.dto.TransactionRequest;
import org.example.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<?> createTransaction(@RequestBody TransactionRequest request) {
        try {
            TransactionDTO result = transactionService.processTransaction(
                    request.getSenderId(),
                    request.getRecipientId(),
                    request.getAmount()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Transaction failed",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<?> getTransactionsByWalletId(@PathVariable String walletId) {
        try {
            List<TransactionDTO> transactions = transactionService.getAllTransactionsById(walletId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "Error", "Bad wallet id"
            ));
        }
    }
}
