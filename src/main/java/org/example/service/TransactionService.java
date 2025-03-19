package org.example.service;

import org.example.entity.Block;
import org.example.entity.Transaction;
import org.example.repository.BlockRepository;
import org.example.repository.TransactionRepository;
import org.example.util.BlockchainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BlockRepository blockRepository;

    @Value("${blockchain.difficulty:5}")
    private Integer difficulty;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, BlockRepository blockRepository) {
        this.transactionRepository = transactionRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional
    public Block processTransaction(Transaction transaction) {

        if (!isChainValid()) {
            throw new BlockchainException("Cannot process transaction: blockchain integrity compromised");
        }

        Block latestBlock = blockRepository.findTopByOrderByTimestampDesc();
        String previousHash = latestBlock != null ? latestBlock.getHash() : "0";


        Block newBlock = new Block(previousHash);
        newBlock.addTransaction(transaction);
        newBlock.mineBlock(difficulty);

        transaction.setBlock(newBlock);

        transactionRepository.save(transaction);
        return blockRepository.save(newBlock);
    }

    public Transaction getTransaction(Integer id) {
        return transactionRepository.getTransactionsById((long) id);
    }

    public boolean isChainValid() {
        List<Block> blockchain = blockRepository.findAllByOrderByTimestampAsc();
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');

        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            Block previousBlock = blockchain.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.applyHash())) {
                return false;
            }

            if (!previousBlock.getHash().equals(previousBlock.applyHash())) {
                return false;
            }

            if (!currentBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                return false;
            }
        }
        return true;
    }
}
