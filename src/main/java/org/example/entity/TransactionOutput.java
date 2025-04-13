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


    public TransactionOutput(PublicKey recipient, double value, String parentTransactionId) {
        this.recipient = recipient;
        this.recipientEncoded = StringUtil.encodeKey(recipient);
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtil.apply(StringUtil.getStringFromKey(recipient) + Double.toString(value) + parentTransactionId);
    }

    public void decodeRecipient() {
        try {
            this.recipient = (PublicKey) StringUtil.decodeKey(recipientEncoded, StringUtil.KeyType.PUBLIC);
        } catch (Exception e) {
            throw new KeyDecodeException("Error decoding keys in txo: " + e.getMessage());
        }
    }

    public boolean isMine(PublicKey publicKey) {
        if (publicKey == null) {
            return false;
        }

        try {
            // First compare encoded values using StringUtil.encodeKey
            String publicKeyEncoded = StringUtil.encodeKey(publicKey);
            if (recipientEncoded.equals(publicKeyEncoded)) {
                return true;
            }

            // As a backup, if we have the actual recipient decoded, try direct comparison
            if (this.recipient != null) {
                String recipientString = StringUtil.getStringFromKey(this.recipient);
                String inputString = StringUtil.getStringFromKey(publicKey);
                return recipientString.equals(inputString);
            }

            return false;
        } catch (Exception e) {
            log.error("Error in isMine method: {}", e.getMessage());
            return false;
        }
    }
}
