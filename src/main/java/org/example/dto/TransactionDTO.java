package org.example.dto;

import lombok.*;
import org.example.entity.Transaction;

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
    private byte[] signature;
    private long timeStamp;

    public static TransactionDTO fromTransaction(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setSender(transaction.getEncodedSenderPublicKey());
        dto.setRecipient(transaction.getEncodedRecipientPublicKey());
        dto.setValue(transaction.getValue());
        dto.setSignature(transaction.getSignature());
        dto.setTimeStamp(transaction.getTimestamp());
        return dto;
    }
}
