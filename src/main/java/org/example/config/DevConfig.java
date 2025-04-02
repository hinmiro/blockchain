package org.example.config;

import org.example.dto.WalletDTO;
import org.example.service.WalletService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DevConfig {

    @Bean
    CommandLineRunner userCommandLineRunner(WalletService walletService) {
        return args -> {
            walletService.addNewWallet(new WalletDTO("m1sc"));
            walletService.addNewWallet(new WalletDTO("s1nzu"));
        };
    }
}
