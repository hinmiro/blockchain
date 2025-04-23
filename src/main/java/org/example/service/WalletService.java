package org.example.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.config.WalletsCreatedEvent;
import org.example.dto.WalletDTO;
import org.example.entity.Transaction;
import org.example.entity.TransactionInput;
import org.example.entity.TransactionOutput;
import org.example.entity.Wallet;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public Wallet addNewWallet(WalletDTO dto) {
        Wallet wallet = new Wallet(dto.getUsername());
        System.out.println(wallet);
        wallet = walletRepository.save(wallet);
        return wallet;

    }

    public WalletDTO getWalletById(String uuid) {
        try {
            Wallet wallet = walletRepository.getWalletById(uuid).orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
            wallet.decodeKeys();
            double currentValue = calculateWalletValue(wallet);
            wallet.setValue(currentValue);

            return convertToDTO(wallet);
        } catch (Exception e) {
            throw new RuntimeException("Wallet not found");
        }
    }

    public WalletDTO convertToDTO(Wallet entity) {
        return new WalletDTO(entity.getId(), entity.getUsername(), entity.getPublicKeyEncoded(), entity.getValue());
    }


    private double calculateWalletValue(Wallet wallet) {
        List<Transaction> transactions = transactionRepository.findAllTransactionOutputsByPublicKey(wallet.getPublicKeyEncoded());

        Set<TransactionOutput> allOutputs = new HashSet<>();
        Set<String> spentOutputIds = new HashSet<>();

        // Collect all outputs for this wallet
        for (Transaction tx : transactions) {
            for (TransactionInput input : tx.getInputs()) {
                spentOutputIds.add(input.getTransactionOutputId());
            }
        }

        double totalValue = 0.0;
        for (Transaction tx : transactions) {
            for (TransactionOutput output : tx.getOutputs()) {
                if (output.getRecipientEncoded().equals(wallet.getPublicKeyEncoded()) && !spentOutputIds.contains(output.getId())) {
                    totalValue += output.getValue();
                }
            }
        }
        return  totalValue;
    }

}
