package org.example;

import org.example.entity.TransactionOutput;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;
import java.util.HashMap;

@SpringBootApplication
public class BlockchainApplication {
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public static void main(String[] args) {
        SpringApplication.run(BlockchainApplication.class, args);
    }
}
