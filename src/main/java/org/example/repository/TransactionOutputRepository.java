package org.example.repository;

import org.example.entity.TransactionOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionOutputRepository extends JpaRepository<TransactionOutput, String> {
    @Query("SELECT o FROM TransactionOutput o WHERE o.recipientEncoded = " +
            "(SELECT w.publicKeyEncoded FROM Wallet w WHERE w.id = :walletId) " +
            "AND o.id NOT IN (SELECT i.transactionOutputId FROM TransactionInput i)")
    List<TransactionOutput> findUnspentOutputsByWalletId(@Param("walletId") String walletId);

}
