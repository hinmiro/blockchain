package org.example.repository;

import org.example.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Transaction getTransactionByTransactionId(String id);


    @Query("SELECT COALESCE(SUM(t.value), 0) FROM Transaction t WHERE t.encodedRecipientPublicKey = :walletId")
    Double sumIncomingTransactions(@Param("walletId") String walletId);

    @Query("SELECT COALESCE(SUM(t.value), 0) FROM Transaction t WHERE t.encodedSenderPublicKey = :walletId")
    Double sumOutgoingTransactions(@Param("walletId") String walletId);

    @Query("SELECT t FROM Transaction t WHERE t.encodedSenderPublicKey = :publicKey OR t.encodedRecipientPublicKey = :publicKey")
    List<Transaction> findAllByWalletPublicKey(@Param("publicKey") String publicKey);
}
