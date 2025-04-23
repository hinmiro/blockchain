package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.util.StringUtil;

import java.math.BigInteger;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@ToString(exclude = "block")
@NoArgsConstructor
public class Transaction {
    @Id
    private String transactionId;

    @Transient
    private PublicKey sender;
    @Transient
    private PublicKey recipient;

    @Column(length = 2000)
    private String encodedSenderPublicKey;

    @Column(length = 2000)
    private String encodedRecipientPublicKey;

    private double value;
    @Column(columnDefinition = "BINARY(64)")
    private byte[] signature;

    @OneToMany(mappedBy = "transaction",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionInput> inputs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionOutput> outputs = new ArrayList<>();

    private long timestamp;
    private static BigInteger sequence = BigInteger.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_hash")
    private Block block;


    public Transaction(PublicKey from, PublicKey to, double value, List<TransactionInput> inputs) {
        this.sender = from;
        this.encodedSenderPublicKey = StringUtil.encodeKey(from);
        this.recipient = to;
        this.encodedRecipientPublicKey = StringUtil.encodeKey(to);
        this.value = value;
        this.inputs = inputs;
        this.timestamp = System.currentTimeMillis();
        this.transactionId = calculateHash();
    }

    public String calculateHash() {
        sequence = sequence.add(BigInteger.ONE);
        return StringUtil.apply(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(recipient) +
                        Double.toString(value) + sequence.toString()

        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Double.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    public boolean verifySignature() {
        String data = encodedSenderPublicKey + encodedRecipientPublicKey + Double.toString(value);
        try {
            PublicKey senderPublicKey = (PublicKey) StringUtil.decodeKey(encodedSenderPublicKey, StringUtil.KeyType.PUBLIC);
            return StringUtil.verifyECDSASig(senderPublicKey, data, this.signature);
        } catch (Exception e) {
            return false;
        }

    }
}
