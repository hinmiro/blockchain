package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.Block;
import org.example.entity.Transaction;
import org.example.repository.BlockRepository;
import org.example.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BlockService {
    private final BlockRepository blockRepository;
    private final MempoolService mempoolService;
    private final TransactionRepository transactionRepository;

    @Value("${blockchain.block.difficulty}")
    private int difficulty;

    @Autowired
    public BlockService(BlockRepository blockRepository, MempoolService mempoolService, TransactionRepository transactionRepository) {
        this.blockRepository = blockRepository;
        this.mempoolService = mempoolService;
        this.transactionRepository = transactionRepository;
    }

    @Scheduled(fixedRate = 10000) // Mine new block every 10 seconds if there are transactions
    @Transactional
    public void mineNewBlockIfNeeded() {
        List<Transaction> transactions = mempoolService.getTransactionsForNewBlock();
        if (!transactions.isEmpty()) {
            try {
                Block latestBlock = blockRepository.findTopByOrderByTimestampDesc()
                        .orElseThrow(() -> new RuntimeException("Genesis block not found"));

                Block newBlock = new Block(latestBlock.getHash());
                transactions.forEach(transaction -> {
                    newBlock.addTransaction(transaction);
                    transaction.setBlock(newBlock);
                });

                newBlock.mineBlock(difficulty);

                Block savedBlock = blockRepository.save(newBlock);
                transactions.forEach(transaction -> {
                    transaction.setBlock(savedBlock);
                    transactionRepository.save(transaction);
                });
                log.info("New block mined and saved: {}", savedBlock.getHash());
            } catch (Exception e) {
                // Return transactions to mempool if mining fails
                transactions.forEach(mempoolService::addTransaction);
                log.error("Block mining failed: {}", e.getMessage());
            }
        }
    }

    public boolean validateBlock(Block block) {
        if (block == null) {
            return false;
        }

        // Use configured difficulty
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        if (!block.getHash().startsWith(hashTarget)) {
            return false;
        }


        // Verify that the stored hash matches a recalculated hash
        String recalculatedHash = block.applyHash();
        if (!block.getHash().equals(recalculatedHash)) {
            return false;
        }

        // Get all blocks ordered by timestamp to verify chain
        List<Block> blockchain = blockRepository.findAllByOrderByTimestampAsc();

        // If not genesis block, verify previous block connection
        if (!block.getPreviousHash().equals("0")) {
            boolean previousBlockFound = blockchain.stream()
                    .anyMatch(b -> b.getHash().equals(block.getPreviousHash()));
            if (!previousBlockFound) {
                return false;
            }
        }

        return true;
    }
}
