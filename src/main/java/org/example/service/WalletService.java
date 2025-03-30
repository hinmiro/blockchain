package org.example.service;

import org.example.dto.WalletDTO;
import org.example.entity.Wallet;
import org.example.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    @Autowired
    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet addNewWallet(WalletDTO dto) {
        Wallet wallet = new Wallet(dto.getUsername());
        return walletRepository.save(wallet);
    }
}
