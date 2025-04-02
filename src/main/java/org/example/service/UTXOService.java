package org.example.service;

import lombok.Getter;
import org.example.entity.TransactionOutput;
import org.example.entity.Wallet;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@Getter
public class UTXOService {
    private HashMap<String, TransactionOutput> UTXOs = new HashMap<>();

    public void addUTXO(TransactionOutput output) {
        UTXOs.put(output.getId(), output);
    }

    public void removeUTXO(String id) {
        UTXOs.remove(id);
    }

    public double getWalletBalance(Wallet wallet) {
        wallet.updateUTXOs(UTXOs);
        return wallet.getBalance();
    }
}
