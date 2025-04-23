package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.util.KeyDecodeException;
import org.example.util.StringUtil;

import java.security.PublicKey;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class TransactionOutput {
    @Id
    @Column(name = "output_id")
    private String id;

    @Transient
    private PublicKey recipient;

    @Column(length = 2000)
    private String recipientEncoded;

    private double value;
    private String parentTransactionId;


    public TransactionOutput(String recipientEncoded, double value, String parentTransactionId) {
        this.recipientEncoded = recipientEncoded;
        this.value = value;
        this.parentTransactionId = parentTransactionId;

        try {
            // First decode the recipient
            this.recipient = (PublicKey) StringUtil.decodeKey(recipientEncoded, StringUtil.KeyType.PUBLIC);
            // Then generate the ID using the decoded recipient
            this.id = StringUtil.apply(StringUtil.getStringFromKey(this.recipient) +
                    Double.toString(value) +
                    parentTransactionId);
        } catch (Exception e) {
            throw new KeyDecodeException("Error decoding recipient key: " + e.getMessage());
        }


    }

    public void decodeRecipient() {
        try {
            this.recipient = (PublicKey) StringUtil.decodeKey(recipientEncoded, StringUtil.KeyType.PUBLIC);
        } catch (Exception e) {
            throw new KeyDecodeException("Error decoding keys in txo: " + e.getMessage());
        }
    }

    public boolean isMine(String publicKey) {
        if (publicKey == null) {
            return false;
        }
        return recipientEncoded.equals(publicKey);
    }
}
