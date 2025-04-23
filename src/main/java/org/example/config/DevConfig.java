package org.example.config;

import jakarta.annotation.PostConstruct;
import org.example.dto.WalletDTO;
import org.example.entity.Transaction;
import org.example.entity.TransactionInput;
import org.example.entity.TransactionOutput;
import org.example.entity.Wallet;
import org.example.repository.TransactionOutputRepository;
import org.example.service.WalletService;
import org.example.util.SecuritySignatureVerification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class DevConfig {

    private final ApplicationEventPublisher eventPublisher;
    private final TransactionOutputRepository outputRepository;

    @Value("${blockchain.genesis.amount}")
    private double genesisAmount;


    public DevConfig(ApplicationEventPublisher eventPublisher, TransactionOutputRepository outputRepository) {
        this.eventPublisher = eventPublisher;
        this.outputRepository = outputRepository;
    }

    @Bean
    CommandLineRunner userCommandLineRunner(WalletService walletService) {
        return args -> {
            Wallet w1 = walletService.addNewWallet(new WalletDTO("m1sc"));
            Wallet w2 = walletService.addNewWallet(new WalletDTO("s1nzu"));

            // Publishing after wallets are created
            eventPublisher.publishEvent(new WalletsCreatedEvent(this));

            Thread.sleep(1000);

            // Get UTXOs for both wallets
            List<TransactionOutput> utxosW1 = outputRepository.findUnspentOutputsByWalletId(w1.getId());
            List<TransactionOutput> utxosW2 = outputRepository.findUnspentOutputsByWalletId(w2.getId());

            System.out.println("\n=== WALLET BALANCES ===");
            System.out.println("Wallet 1 (m1sc) balance: " + utxosW1.stream().mapToDouble(TransactionOutput::getValue).sum());
            System.out.println("Wallet 2 (s1nzu) balance: " + utxosW2.stream().mapToDouble(TransactionOutput::getValue).sum());


            List<String> inputIds = utxosW1.stream()
                    .map(TransactionOutput::getId)
                    .toList();


            String testSignature = Base64.getEncoder().encodeToString("test_signature".getBytes());

            System.out.println("\n=== TEST TRANSACTION INFO ===");
            System.out.println("Sender wallet ID: " + w1.getId());
            System.out.println("Recipient wallet ID: " + w2.getId());
            System.out.println("Available UTXO IDs for sender: " + inputIds);

            System.out.println("\nExample POST request to: http://localhost:8080/api/v1/transaction/" + w1.getId());
            System.out.println("Headers:");
            System.out.println("X-Wallet-Signature: WALLET_ACCESS:" + w1.getId());
            System.out.println("\nRequest body:");
            System.out.println("{\n" +
                    "    \"recipientId\": \"" + w2.getId() + "\",\n" +
                    "    \"amount\": 2.5,\n" +
                    "    \"inputTransactionIds\": " + inputIds + ",\n" +
                    "    \"signature\": \"" + testSignature + "\"\n" +
                    "}");


        };
    }
}
