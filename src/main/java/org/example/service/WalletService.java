package org.example.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.dto.WalletDTO;
import org.example.entity.Wallet;
import org.example.repository.TransactionRepository;
import org.example.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return walletRepository.save(wallet);
    }

    public WalletDTO getWalletById(String uuid) {
        try {
            Wallet wallet = walletRepository.getWalletById(uuid).orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
            wallet.decodeKeys();
            return convertToDTO(wallet);
        } catch (Exception e) {
            throw new RuntimeException("Wallet not found");
        }
    }

    public WalletDTO convertToDTO(Wallet entity) {
        WalletDTO conversion = new WalletDTO(entity.getId(), entity.getUsername(), entity.getPublicKeyEncoded(), entity.getValue());
        conversion.setValue(calculateWalletBalance(conversion.getId()));
        return conversion;
    }

    private Double calculateWalletBalance(String walletId) {
        Double incoming = transactionRepository.sumIncomingTransactions(walletId);
        Double outgoing = transactionRepository.sumOutgoingTransactions(walletId);

        return incoming - outgoing;
    }
}
