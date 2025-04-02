package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.config.WalletsCreatedEvent;
import org.example.dto.TransactionDTO;
import org.example.entity.*;
import org.example.repository.BlockRepository;
import org.example.repository.TransactionOutputRepository;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.example.util.BlockchainException;
import org.example.util.StringUtil;
import org.example.util.TransactionForgeryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BlockRepository blockRepository;
    private final WalletRepository walletRepository;
    private final TransactionOutputRepository outputRepository;

    private Map<String, TransactionOutput> UTXOs = new HashMap<>();

    @Value("${blockchain.difficulty:1}")
    private Integer difficulty;

    @Value("${blockchain.genesis.enabled:true}")
    private boolean genesisEnabled;

    @Value("${blockchain.genesis.amount:1}")
    private double genesisAmount;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, BlockRepository blockRepository, WalletRepository walletRepository, TransactionOutputRepository outputRepository) {
        this.transactionRepository = transactionRepository;
        this.blockRepository = blockRepository;
        this.walletRepository = walletRepository;
        this.outputRepository = outputRepository;
    }

    @PostConstruct
    public void initialize() {
        initializeUTXOs();
    }

    @EventListener
    public void handleWalletCreatedEvent(WalletsCreatedEvent event) {
        if (genesisEnabled) {
            log.info("Wallets are ready, creating genesis transactions...");
            createGenesisTransactions();
        }
    }

    private void initializeUTXOs() {
        outputRepository.findAll().forEach(output -> {
            output.decodeRecipient();
            UTXOs.put(output.getId(), output);
        });
    }

    @Transactional
    public TransactionDTO processTransaction(String senderId, String recipientId, double amount) {
        System.out.println("Values: " + senderId + " " + recipientId + " " + amount);
        // Check validity of blockchain
        if (!isChainValid()) {
            throw new BlockchainException("Cannot process transaction: blockchain integrity compromised");
        }

        Wallet senderWallet = walletRepository.findById(senderId).orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet recipientWallet = walletRepository.findById(recipientId).orElseThrow(() -> new IllegalArgumentException("Recipient wallet not found"));

        senderWallet.decodeKeys();
        recipientWallet.decodeKeys();

        Transaction transaction = new Transaction(
                senderWallet.getPublicKey(),
                recipientWallet.getPublicKey(),
                amount, null
        );

        System.out.println("Transaction object: " + transaction);
        // Check that wallets exists
        if (!transactionKeyCheck(senderWallet.getPublicKeyEncoded(), recipientWallet.getPublicKeyEncoded())) {
            throw new TransactionForgeryException("Transactions forgery attempt");
        }

        // Find sender UTXOs and create inputs
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        double inputTotal = 0;

        for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
            TransactionOutput utxo = item.getValue();
            if (utxo.isMine(senderWallet.getPublicKey())) {
                inputTotal += utxo.getValue();
                inputs.add(new TransactionInput(utxo.getId()));
                if (inputTotal >= transaction.getValue()) break;
            }
        }

        // Check funds
        if (inputTotal < transaction.getValue()) {
            throw new TransactionForgeryException("Insufficient funds: " + inputTotal);
        }

        transaction.setInputs(inputs);

        // Transaction signing
        PrivateKey senderPrivateKey = (PrivateKey) StringUtil.decodeKey(senderWallet.getPrivateKeyEncoded(), StringUtil.KeyType.PRIVATE);
        transaction.generateSignature(senderPrivateKey);

        // Check verifying
        if (!transaction.verifySignature()) {
            throw new TransactionForgeryException("Transaction signature failed to verify");
        }

        // Create outputs
        double leftOver = inputTotal - transaction.getValue();

        // Output to recipient
        TransactionOutput outputToRecipient = new TransactionOutput(
                transaction.getRecipient(),
                transaction.getValue(),
                transaction.getTransactionId()
        );
        transaction.getOutputs().add(outputToRecipient);

        // Back to sender
        if (leftOver > 0) {
            TransactionOutput outputToSender = new TransactionOutput(
                    transaction.getSender(),
                    leftOver,
                    transaction.getTransactionId()
            );
            transaction.getOutputs().add(outputToSender);
        }

        // Add transaction to block
        Block latestBlock = blockRepository.findTopByOrderByTimestampDesc();
        String previousHash = latestBlock != null ? latestBlock.getHash() : "0";
        Block newBlock = new Block(previousHash);
        newBlock.addTransaction(transaction);
        newBlock.mineBlock(difficulty);

        // Update UTXO pool
        for (TransactionOutput output : transaction.getOutputs()) {
            UTXOs.put(output.getId(), output);
        }

        // Remove spent outputs
        for (TransactionInput input : transaction.getInputs()) {
            UTXOs.remove(input.getTransactionOutputId());
        }

        // Save everything
        newBlock = blockRepository.save(newBlock);
        // So block needs to be in db before transaction
        transaction.setBlock(newBlock);
        transactionRepository.save(transaction);

        return TransactionDTO.fromTransaction(transaction);
    }

    public Transaction getTransaction(String id) {
        return transactionRepository.getTransactionByTransactionId(id);
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

    private boolean transactionKeyCheck(String senderKey, String recipientKey) {
        return walletRepository.existsByPublicKeyEncoded(senderKey) && walletRepository.existsByPublicKeyEncoded(recipientKey);
    }

    public Transaction convertToEntity(TransactionDTO dto) {
        PublicKey recipientKey = (PublicKey) StringUtil.decodeKey(dto.getRecipient(), StringUtil.KeyType.PUBLIC);
        PublicKey senderKey = (PublicKey) StringUtil.decodeKey(dto.getSender(), StringUtil.KeyType.PUBLIC);
        return new Transaction(senderKey, recipientKey, dto.getValue(), null);
    }

    // Only for testing
    private void createGenesisTransactions() {
        try {
            List<Wallet> wallets = walletRepository.findAll();
            log.info("Found {} wallets for genesis transactions", wallets.size());

            if (wallets.isEmpty()) {
                log.warn("No wallets found, skipping genesis transactions");
                return;
            }

            Block genesisBlock = blockRepository.findTopByOrderByTimestampAsc()
                    .orElseGet(() -> {
                        Block newBlock = new Block("0");
                        return blockRepository.save(newBlock);
                    });

            for (Wallet wallet : wallets) {
                wallet.decodeKeys();

                // Create a coinbase transaction (no inputs, only outputs)
                Transaction genesisTx = new Transaction();
                genesisTx.setTransactionId(UUID.randomUUID().toString());
                genesisTx.setBlock(genesisBlock);
                genesisTx.setTimestamp(System.currentTimeMillis());

                // Create output to this wallet
                TransactionOutput output = new TransactionOutput(
                        wallet.getPublicKey(),
                        genesisAmount,
                        genesisTx.getTransactionId()
                );

                genesisTx.getOutputs().add(output);
                transactionRepository.save(genesisTx);
                outputRepository.save(output);

                UTXOs.put(output.getId(), output);

                log.info("Added {} coins to wallet {}", genesisAmount, wallet.getId());
            }

        } catch (Exception e) {
            log.error("Error creating genesis transactions: {}", e.getMessage(), e);
            throw new RuntimeException("Error in genesis creation: " + e.getMessage());
        }
    }
}
