package org.example.repository;

import org.example.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    Wallet getWalletById(String uuid);

    boolean existsByPublicKeyEncoded(String publicKey);
}
