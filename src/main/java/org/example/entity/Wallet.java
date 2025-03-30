package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Entity
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
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

            privateKeyEncoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            publicKeyEncoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Key generation exception: " + e.getMessage());
        }
    }
}
