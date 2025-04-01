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
        System.out.println(wallet);
        return walletRepository.save(wallet);
    }

    public WalletDTO getWalletById(String uuid) {
        try {
            Wallet wallet = walletRepository.getWalletById(uuid);
            wallet.decodeKeys();
            System.out.println(wallet);
            return convertToDTO(wallet);
        } catch (Exception e) {
            throw new RuntimeException("Wallet not found");
        }
    }

    public WalletDTO convertToDTO(Wallet entity) {
        WalletDTO conversion = new WalletDTO(entity.getId(), entity.getUsername(), entity.getPublicKeyEncoded(), entity.getValue());
        System.out.println("Converted: " + conversion);
        return conversion;
    }
}
