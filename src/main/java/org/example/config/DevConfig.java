package org.example.config;

import jakarta.annotation.PostConstruct;
import org.example.dto.WalletDTO;
import org.example.service.WalletService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DevConfig {

    private final ApplicationEventPublisher eventPublisher;

    public DevConfig(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Bean
    CommandLineRunner userCommandLineRunner(WalletService walletService) {
        return args -> {
            walletService.addNewWallet(new WalletDTO("m1sc"));
            walletService.addNewWallet(new WalletDTO("s1nzu"));

            // Publishing after wallets are created
            eventPublisher.publishEvent(new WalletsCreatedEvent(this));
        };
    }
}
