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
    private Double value;
    @Transient
    private PrivateKey privateKey;
    @Transient
    private PublicKey publicKey;

    @Column(length = 2000)
    private String privateKeyEncoded;

    @Column(length = 2000)
    private String publicKeyEncoded;

    public Wallet(String username) {
        this.username = username;
        this.value = 0.00;
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");

            keyGen.initialize(ecSpec, random);
            KeyPair keyPair = keyGen.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            privateKeyEncoded = StringUtil.encodeKey(privateKey);
            publicKeyEncoded = StringUtil.encodeKey(publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Key generation exception: " + e.getMessage());
        }
    }

    public void decodeKeys() {
        try {
            this.publicKey = (PublicKey) StringUtil.decodeKey(publicKeyEncoded, StringUtil.KeyType.PUBLIC);
            this.privateKey = (PrivateKey) StringUtil.decodeKey(privateKeyEncoded, StringUtil.KeyType.PRIVATE);
        } catch (Exception e) {
            throw new KeyDecodeException("Corrupted key encoding: " + e.getMessage());
        }
    }
}
