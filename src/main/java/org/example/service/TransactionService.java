package org.example.service;

import org.example.dto.TransactionDTO;
import org.example.entity.Block;
import org.example.entity.Transaction;
import org.example.entity.Wallet;
import org.example.repository.BlockRepository;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.example.util.BlockchainException;
import org.example.util.StringUtil;
import org.example.util.TransactionForgeryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BlockRepository blockRepository;
    private final WalletRepository walletRepository;

    @Value("${blockchain.difficulty:5}")
    private Integer difficulty;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, BlockRepository blockRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.blockRepository = blockRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public TransactionDTO processTransaction(TransactionDTO dto) {

        // Check validity of blockchain
        if (!isChainValid()) {
            throw new BlockchainException("Cannot process transaction: blockchain integrity compromised");
        }

        Transaction transaction = convertToEntity(dto);

        // Encode to Base64
        String encodedSender = StringUtil.encodeKey(transaction.getSender());
        String encodedRecipient = StringUtil.encodeKey(transaction.getRecipient());

        transaction.setEncodedSenderPublicKey(encodedSender);
        transaction.setEncodedRecipientPublicKey(encodedRecipient);

        // Check that wallets exists
        if (!transactionKeyCheck(encodedSender, encodedRecipient)) {
            throw new TransactionForgeryException("Transactions forgery attempt");
        }

        Block latestBlock = blockRepository.findTopByOrderByTimestampDesc();
        String previousHash = latestBlock != null ? latestBlock.getHash() : "0";


        Block newBlock = new Block(previousHash);
        newBlock.addTransaction(transaction);
        newBlock.mineBlock(difficulty);

        transaction.setBlock(newBlock);

        // Transaction signing
        Wallet senderWallet = walletRepository.getWalletByPublicKeyEncoded(dto.getSender());
        PrivateKey senderPrivateKey = (PrivateKey) StringUtil.decodeKey(senderWallet.getPrivateKeyEncoded(), StringUtil.KeyType.PRIVATE);
        transaction.generateSignature(senderPrivateKey);

        // Check verifying
        transaction.verifySignature();

        transactionRepository.save(transaction);
        blockRepository.save(newBlock);
        return TransactionDTO.fromTransaction(transaction);
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

    private boolean transactionKeyCheck(String senderKey, String recipientKey) {
        return walletRepository.existsByPublicKeyEncoded(senderKey) && walletRepository.existsByPublicKeyEncoded(recipientKey);
    }

    public Transaction convertToEntity(TransactionDTO dto) {
        System.out.println("CONVERSION START");
        PublicKey recipientKey = (PublicKey) StringUtil.decodeKey(dto.getRecipient(), StringUtil.KeyType.PUBLIC);
        PublicKey senderKey = (PublicKey) StringUtil.decodeKey(dto.getSender(), StringUtil.KeyType.PUBLIC);
        System.out.println("Conversion done");
        return new Transaction(senderKey, recipientKey, dto.getValue(), null);
    }
}
