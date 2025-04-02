package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.util.KeyDecodeException;
import org.example.util.StringUtil;

import java.security.PublicKey;

@Entity
@NoArgsConstructor
@Getter
@Setter
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
        return (publicKey != null && publicKey.equals(recipient));
    }
}
