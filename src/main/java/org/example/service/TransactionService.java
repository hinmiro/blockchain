package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.config.WalletsCreatedEvent;
import org.example.dto.TransactionDTO;
import org.example.dto.WalletDTO;
import org.example.entity.*;
import org.example.repository.BlockRepository;
import org.example.repository.TransactionOutputRepository;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.example.util.BlockchainException;
import org.example.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final WalletService walletService;
    private final TransactionOutputRepository transactionOutputRepository;
    private final MempoolService mempoolService;

    private Map<String, TransactionOutput> UTXOs = new HashMap<>();

    @Value("${blockchain.block.difficulty}")
    private int difficulty;


    @Value("${blockchain.genesis.enabled:true}")
    private boolean genesisEnabled;

    @Value("${blockchain.genesis.amount:1}")
    private double genesisAmount;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, BlockRepository blockRepository, WalletRepository walletRepository, TransactionOutputRepository outputRepository, WalletService walletService, TransactionOutputRepository transactionOutputRepository, MempoolService mempoolService) {
        this.transactionRepository = transactionRepository;
        this.blockRepository = blockRepository;
        this.walletRepository = walletRepository;
        this.outputRepository = outputRepository;
        this.walletService = walletService;
        this.transactionOutputRepository = transactionOutputRepository;
        this.mempoolService = mempoolService;
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
    public TransactionDTO processTransaction(String senderId, String recipientId, double amount, String transactionSignature) {
        // Check validity of the blockchain
        if (!isChainValid()) {
            throw new BlockchainException("Cannot process transaction: blockchain integrity compromised");
        }

        try {
            // Get and validate wallets
            WalletDTO senderWallet = walletService.getWalletById(senderId);
            WalletDTO recipientWallet = walletService.getWalletById(recipientId);

            // Create transaction input from utxos
            List<TransactionOutput> availableUTXOs = transactionOutputRepository.findUnspentOutputsByWalletId(senderId);
            double availableBalance = availableUTXOs.stream().mapToDouble(TransactionOutput::getValue).sum();

            if (availableBalance < amount) {
                throw new BlockchainException("Insufficient funds. Available: " + availableBalance + ", Required: " + amount);
            }

            // Select UTXOs for this transaction
            double currentSum = 0;
            List<TransactionInput> selectedInputs = new ArrayList<>();
            for (TransactionOutput utxo : availableUTXOs) {
                currentSum += utxo.getValue();
                selectedInputs.add(new TransactionInput(utxo.getId()));
                if (currentSum >= amount) {
                    break;
                }
            }


            // Create and verify transaction
            Transaction newTransaction = new Transaction(
                    (PublicKey) StringUtil.decodeKey(senderWallet.getPublicKeyEncoded(), StringUtil.KeyType.PUBLIC),
                    (PublicKey) StringUtil.decodeKey(recipientWallet.getPublicKeyEncoded(), StringUtil.KeyType.PUBLIC),
                    amount,
                    selectedInputs
            );

            newTransaction.setSignature(Base64.getDecoder().decode(transactionSignature));

            if (!newTransaction.verifySignature()) {
                throw new BlockchainException("Transaction signature verification failed");
            }

            createTransactionOutputs(newTransaction, senderWallet.getPublicKeyEncoded(), recipientWallet.getPublicKeyEncoded(), amount, currentSum);

            Transaction savedTransaction = transactionRepository.save(newTransaction);

            // Save each output
            for (TransactionOutput output : savedTransaction.getOutputs()) {
                outputRepository.save(output);
                UTXOs.put(output.getId(), output);
            }

            mempoolService.addTransaction(newTransaction);

            // Create a new block and mine it
            // Block block = createBlockForTransaction(newTransaction);

            return convertToDto(newTransaction);
        } catch (Exception e) {
            throw new RuntimeException("Transaction processing failed: " + e.getMessage());
        }
    }


    private double processTransactionInputs(Transaction transaction, String senderPublicKey) {
        return transaction.getInputs().stream()
                .map(input -> transactionOutputRepository.findById(input.getTransactionOutputId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(output -> output.isMine(senderPublicKey))
                .mapToDouble(TransactionOutput::getValue)
                .sum();
    }


    private void createTransactionOutputs(Transaction transaction, String senderPublicKey, String recipientPublicKey, double amount, double inputSum) {
        // Create output for a recipient
        transaction.getOutputs().add(new TransactionOutput(
                recipientPublicKey,
                amount,
                transaction.getTransactionId()
        ));

        // Create changes if needed
        double change = inputSum - amount;
        if (change > 0) {
            transaction.getOutputs().add(new TransactionOutput(
                    senderPublicKey,
                    change,
                    transaction.getTransactionId()
            ));
        }


    }

    @Transactional
    protected Block createBlockForTransaction(Transaction transaction) {
        // Find the latest block
        Block latestBlock = blockRepository.findTopByOrderByTimestampDesc()
                .orElseThrow(() -> new RuntimeException("Genesis block not found"));

        // Create new block
        Block newBlock = new Block(latestBlock.getHash());
        newBlock.addTransaction(transaction);

        // Mine the block
        log.info("Mining block for transaction: {}", transaction.getTransactionId());
        newBlock.mineBlock(difficulty);

        // Save block and update transaction
        Block savedBlock = blockRepository.save(newBlock);
        transaction.setBlock(savedBlock);
        transactionRepository.save(transaction);

        log.info("Block created and mined: {}", savedBlock.getHash());
        return savedBlock;
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
                        log.info("Creating new genesis block");
                        Block newBlock = new Block("0");
                        return blockRepository.save(newBlock);
                    });

            for (Wallet wallet : wallets) {
                wallet.decodeKeys();
                log.info("Creating genesis transaction for wallet: {} with public key: {}",
                        wallet.getId(), wallet.getPublicKeyEncoded());


                // Create a coinbase transaction (no inputs, only outputs)
                Transaction genesisTx = new Transaction();
                genesisTx.setTransactionId(UUID.randomUUID().toString());
                genesisTx.setBlock(genesisBlock);
                genesisTx.setTimestamp(System.currentTimeMillis());
                genesisTx.setEncodedRecipientPublicKey(wallet.getPublicKeyEncoded());
                genesisTx.setEncodedSenderPublicKey("Genesis");

                // Create output to this wallet
                TransactionOutput output = new TransactionOutput(
                        wallet.getPublicKeyEncoded(),
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

    private TransactionDTO convertToDto(Transaction transaction) {
        return TransactionDTO.fromTransaction(transaction);
    }
}
