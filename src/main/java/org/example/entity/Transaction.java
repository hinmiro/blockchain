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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;
    private PublicKey sender;
    private PublicKey recipient;
    private double value;
    private long timestamp;
    private byte[] signature;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "input_id")
    private List<TransactionInput> inputs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "output_id")
    private List<TransactionOutput> outputs = new ArrayList<>();

    private static BigInteger sequence = BigInteger.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_hash")
    private Block block;


    public Transaction(PublicKey from, PublicKey to, double value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
        this.timestamp = System.currentTimeMillis();
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
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Double.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }
}
