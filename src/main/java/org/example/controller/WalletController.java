package org.example.controller;

import org.example.dto.WalletDTO;
import org.example.entity.Wallet;
import org.example.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    @Autowired
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<?> createNewWallet(@RequestBody WalletDTO dto) {
        try {
            Wallet newWallet = walletService.addNewWallet(dto);
            return ResponseEntity.ok(Map.of(
                    "walletId", newWallet.getId(),
                    "walletPublicKey", newWallet.getPublicKeyEncoded()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "creation of new wallet failed",
                    "message", e.getCause()
            ));
        }
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<?> getWallet(@PathVariable String walletId) {
        try {
            WalletDTO wallet = walletService.getWalletById(walletId);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "Error", "Bad wallet"
            ));
        }
    }
}
