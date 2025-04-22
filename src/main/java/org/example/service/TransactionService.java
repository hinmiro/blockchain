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
import java.util.stream.Collectors;

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
        // Check validity of blockchain
        if (!isChainValid()) {
            throw new BlockchainException("Cannot process transaction: blockchain integrity compromised");
        }

        // Get and validate wallets
        Wallet senderWallet = walletRepository.findById(senderId).orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet recipientWallet = walletRepository.findById(recipientId).orElseThrow(() -> new IllegalArgumentException("Recipient wallet not found"));

        senderWallet.decodeKeys();
        recipientWallet.decodeKeys();

        if (!transactionKeyCheck(senderWallet.getPublicKeyEncoded(), recipientWallet.getPublicKeyEncoded())) {
            throw new TransactionForgeryException("Transactions forgery attempt");
        }

        // Create transaction
        Transaction transaction = new Transaction(
                senderWallet.getPublicKey(),
                recipientWallet.getPublicKey(),
                amount, null
        );

        // Find and collect inputs
        List<TransactionInput> inputs = new ArrayList<>();
        double inputTotal = collectInputsForTransaction(senderWallet.getPublicKey(), amount, inputs);

        if (inputTotal < amount) {
            throw new TransactionForgeryException("Insufficient funds: " + inputTotal);
        }

        transaction.setInputs(inputs);

        for (TransactionInput input : inputs) {
            input.setTransaction(transaction);
        }

        // Sign transaction
        PrivateKey senderPrivateKey = (PrivateKey) StringUtil.decodeKey(senderWallet.getPrivateKeyEncoded(), StringUtil.KeyType.PRIVATE);
        transaction.generateSignature(senderPrivateKey);

        if (!transaction.verifySignature()) {
            throw new TransactionForgeryException("Transaction signature failed to verify");
        }

        // Generate outputs
        double leftOver = inputTotal - amount;
        createTransactionOutputs(transaction, amount, leftOver);

        // Add to blockchain
        Block newBlock = createBlockForTransaction(transaction);

        // Update UTXO pool
        updateUTXOPool(transaction);

        return TransactionDTO.fromTransaction(transaction);
    }

    private double collectInputsForTransaction(PublicKey senderPublicKey, double targetAmount, List<TransactionInput> inputs) {
        double total = 0;
        int utxoCount = 0;
        int totalUTXOs = UTXOs.size();

        log.info("Collecting inputs for transaction. Target amount: {}", targetAmount);
        log.info("Total UTXOs in pool: {}", totalUTXOs);

        for (Map.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput utxo = entry.getValue();
            boolean isMine = utxo.isMine(senderPublicKey);

            if (isMine) {
                utxoCount++;
                total += utxo.getValue();
                inputs.add(new TransactionInput(utxo.getId()));
                log.debug("Added UTXO {} to inputs. Value: {}, Running total: {}",
                        utxo.getId(), utxo.getValue(), total);
                if (total >= targetAmount) break;
            }
        }

        log.info("Found {} UTXOs for wallet, total value: {}", utxoCount, total);

        if (utxoCount == 0) {
            // Debug the UTXOs for troubleshooting
            log.warn("No UTXOs found for this wallet. Public key: {}",
                    StringUtil.getStringFromKey(senderPublicKey));

            int count = 0;
            for (Map.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
                TransactionOutput utxo = entry.getValue();
                log.debug("UTXO {}: Recipient encoded: {}, Is mine check: {}",
                        count++, utxo.getRecipientEncoded(), utxo.isMine(senderPublicKey));
                if (count > 10) break;
            }
        }

        return total;
    }

    private void createTransactionOutputs(Transaction transaction, double amount, double leftOver) {
        // Output to recipient
        TransactionOutput outputToRecipient = new TransactionOutput(
                transaction.getRecipient(),
                amount,
                transaction.getTransactionId()
        );
        transaction.getOutputs().add(outputToRecipient);

        // Change back to sender if needed
        if (leftOver > 0) {
            TransactionOutput outputToSender = new TransactionOutput(
                    transaction.getSender(),
                    leftOver,
                    transaction.getTransactionId()
            );
            transaction.getOutputs().add(outputToSender);
        }
    }

    private Block createBlockForTransaction(Transaction transaction) {
        Block latestBlock = blockRepository.findTopByOrderByTimestampDesc();
        String previousHash = latestBlock != null ? latestBlock.getHash() : "0";

        Block newBlock = new Block(previousHash);
        newBlock.addTransaction(transaction);
        newBlock.mineBlock(difficulty);

        Block savedBlock = blockRepository.save(newBlock);
        transaction.setBlock(savedBlock);
        transactionRepository.save(transaction);

        return savedBlock;
    }

    private void updateUTXOPool(Transaction transaction) {
        // Remove spent outputs
        for (TransactionInput input : transaction.getInputs()) {
            TransactionOutput removed = UTXOs.remove(input.getTransactionOutputId());
            log.info("Removed UTXO: {} with value {}",
                    input.getTransactionOutputId(),
                    removed != null ? removed.getValue() : "null");
        }

        // Add new outputs
        for (TransactionOutput output : transaction.getOutputs()) {
            TransactionOutput savedOutput = outputRepository.save(output);
            UTXOs.put(savedOutput.getId(), savedOutput);
            log.info("Added UTXO: {} with value {} for recipient {}",
                    savedOutput.getId(),
                    savedOutput.getValue(),
                    savedOutput.getRecipientEncoded().substring(0, 20) + "...");
        }

        log.info("UTXO pool now contains {} outputs", UTXOs.size());
    }


    public boolean isChainValid() {
        List<Block> blockchain = blockRepository.findAllBlocksWithTransactions();
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');

        if (blockchain.size() <= 1) {
            return true; // Only genesis block exists, nothing to validate
        }

        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            Block previousBlock = blockchain.get(i - 1);
            String recalculatedHash = currentBlock.applyHash();


            if (!currentBlock.getHash().equals(recalculatedHash)) {
                log.error("Current block hash invalid. Block: {}, Expected: {}, Got: {}",
                        currentBlock.getTimestamp(), currentBlock.getHash(), recalculatedHash);
                return false;
            }

            if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                log.error("Previous hash reference invalid. Block: {}, Expected: {}, Got: {}",
                        currentBlock.getTimestamp(), previousBlock.getHash(), currentBlock.getPreviousHash());
                return false;
            }

            if (!currentBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                log.error("Block doesn't meet difficulty requirement. Block: {}", currentBlock.getTimestamp());
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
                genesisTx.setEncodedRecipientPublicKey(wallet.getPublicKeyEncoded());
                genesisTx.setEncodedSenderPublicKey("Genesis");

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

    public List<TransactionDTO> getAllTransactionsById(String id) {
        Wallet wallet = walletRepository.findById(id).orElseThrow(() -> new RuntimeException("Invalid wallet id"));

        List<Transaction> transactions = transactionRepository.findAllByWalletPublicKey(wallet.getPublicKeyEncoded());

        return convertToTransactionDTOs(transactions);
    }

    public List<TransactionDTO> convertToTransactionDTOs(List<Transaction> transactions) {
        return transactions.stream()
                .map(TransactionDTO::fromTransaction)
                .collect(Collectors.toList());
    }
}
