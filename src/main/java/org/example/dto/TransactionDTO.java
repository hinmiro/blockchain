package org.example.dto;

import lombok.*;
import org.example.entity.Transaction;

import java.util.Base64;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class TransactionDTO {
    private String transactionId;
    private String sender;
    private String recipient;
    private Double value;
    private String signature;
    private long timeStamp;

    public TransactionDTO(String publicKeyEncoded, String publicKeyEncoded1, double amount) {
    }

    public static TransactionDTO fromTransaction(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setSender(transaction.getEncodedSenderPublicKey());
        dto.setRecipient(transaction.getEncodedRecipientPublicKey());
        dto.setValue(transaction.getValue());
        dto.setSignature(Base64.getEncoder().encodeToString(transaction.getSignature()));
        dto.setTimeStamp(transaction.getTimestamp());
        return dto;
    }
}
