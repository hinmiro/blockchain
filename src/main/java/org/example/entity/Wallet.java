package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.util.KeyDecodeException;
import org.example.util.StringUtil;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.Map;

@Entity
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String username;
    @Transient
    private Double value;
    @Transient
    private PublicKey publicKey;

    @Column(length = 2000)
    private String publicKeyEncoded;

    @Transient
    private Map<String, TransactionOutput> UTXOs = new HashMap<>();

    public Wallet(String username) {
        this.username = username;
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");

            keyGen.initialize(ecSpec, random);
            KeyPair keyPair = keyGen.generateKeyPair();

            publicKey = keyPair.getPublic();

            publicKeyEncoded = StringUtil.encodeKey(publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Key generation exception: " + e.getMessage());
        }
    }

    public void decodeKeys() {
        try {
            this.publicKey = (PublicKey) StringUtil.decodeKey(publicKeyEncoded, StringUtil.KeyType.PUBLIC);
        } catch (Exception e) {
            throw new KeyDecodeException("Corrupted key encoding: " + e.getMessage());
        }
    }

    public double getBalance() {
        double total = 0;
        for (TransactionOutput utxo : UTXOs.values()) {
            total += utxo.getValue();
        }
        return total;
    }

    public void updateUTXOs(Map<String, TransactionOutput> globalUTXOs) {
        UTXOs.clear();
        for (TransactionOutput output : globalUTXOs.values()) {
            if (output.isMine(StringUtil.encodeKey(publicKey))) {
                UTXOs.put(output.getId(), output);
            }
        }
    }
}
