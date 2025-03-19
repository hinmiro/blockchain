package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.example.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BlockchainValidator {
    private final TransactionService transactionService;

    @Autowired
    public BlockchainValidator(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(fixedRate = 600000)
    public void validateBlockchain() {
        boolean valid = transactionService.isChainValid();
        if (!valid) {
            log.error("BLOCKCHAIN INTEGRITY COMPROMISED - System may be under attack or data corruption occurred");
            throw new BlockchainException("Blockchain integrity compromised");
        }
    }
}
