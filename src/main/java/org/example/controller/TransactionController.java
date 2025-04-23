package org.example.controller;

import org.example.dto.TransactionDTO;
import org.example.dto.TransactionRequest;
import org.example.dto.WalletDTO;
import org.example.service.TransactionService;
import org.example.service.WalletService;
import org.example.util.SecuritySignatureVerification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/transaction")
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletService walletService;

    @Autowired
    public TransactionController(TransactionService transactionService, WalletService walletService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    @PostMapping("/{walletId}")
    public ResponseEntity<?> createTransaction(@PathVariable String walletId, @RequestHeader("X-Wallet-Signature") String walletSignature, @RequestBody TransactionRequest request) {
        try {
            // Verification
            WalletDTO senderWallet = walletService.getWalletById(walletId);
            if (!SecuritySignatureVerification.verifyWalletAccess(senderWallet, walletSignature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Process transaction
            TransactionDTO transaction = transactionService.processTransaction(
                    walletId,
                    request.getRecipientId(),
                    request.getAmount(),
                    request.getSignature()
            );

            return ResponseEntity.ok(transaction);
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
