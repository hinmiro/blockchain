package org.example.service;

import org.example.entity.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class MempoolService {
    private final Queue<Transaction> pendingTransactions = new ConcurrentLinkedDeque<>();
    private int maxTransactionsPerBlock;

    @Value("${blockchain.block.max-transactions}")
    public void setMaxTransactionsPerBlock(int maxTransactionsPerBlock) {
        this.maxTransactionsPerBlock = maxTransactionsPerBlock;
    }

    public void addTransaction(Transaction transaction) {
        pendingTransactions.offer(transaction);
    }

    public List<Transaction> getTransactionsForNewBlock() {
        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction;
        while (transactions.size() < maxTransactionsPerBlock && (transaction = pendingTransactions.poll()) != null) {
            transactions.add(transaction);
        }
        return transactions;
    }
}
