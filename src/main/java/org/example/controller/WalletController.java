package org.example.controller;

import org.example.dto.TransactionOutputDTO;
import org.example.dto.WalletDTO;
import org.example.entity.TransactionOutput;
import org.example.entity.Wallet;
import org.example.repository.TransactionOutputRepository;
import org.example.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/wallet")
public class WalletController {

    private final WalletService walletService;
    private final TransactionOutputRepository transactionOutputRepository;

    @Autowired
    public WalletController(WalletService walletService, TransactionOutputRepository transactionOutputRepository) {
        this.walletService = walletService;
        this.transactionOutputRepository = transactionOutputRepository;
    }

    @PostMapping
    public ResponseEntity<?> createNewWallet(@RequestBody WalletDTO dto) {
        try {

            if (dto.getPublicKeyEncoded() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Public key is required"
                ));
            }

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
    public ResponseEntity<?> getWallet(@PathVariable String walletId, @RequestHeader("X-Wallet-Signature") String signature) {

        try {
            WalletDTO wallet = walletService.getWalletById(walletId);

            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "Error", "Bad wallet"
            ));
        }
    }

    @GetMapping("/{walletId}/utxos")
    public ResponseEntity<List<TransactionOutputDTO>> getWalletUTXOs(@PathVariable String walletId) {
        List<TransactionOutput> utxos = transactionOutputRepository.findUnspentOutputsByWalletId(walletId);
        return ResponseEntity.ok(utxos.stream()
                .map(TransactionOutputDTO::fromEntity)
                .collect(Collectors.toList()));
    }

}
