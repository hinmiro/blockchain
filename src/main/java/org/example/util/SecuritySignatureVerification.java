package org.example.util;

import org.example.dto.WalletDTO;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class SecuritySignatureVerification {
    private static final long TRANSACTION_SIGNATURE_VALIDITY = 5 * 60 * 1000; // 5 minutes

    public static String generateWalletAccessSignature(PrivateKey privateKey, String walletId) {
        try {
            // Create a permanent signature for wallet access
            String signatureData = "WALLET_ACCESS:" + walletId;
            byte[] signature = StringUtil.applyECDSASig(privateKey, signatureData);
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature");
        }
    }

    public static boolean verifyWalletAccess(WalletDTO wallet, String signature) {
        try {
            PublicKey publicKey = (PublicKey) StringUtil.decodeKey(wallet.getPublicKeyEncoded(), StringUtil.KeyType.PUBLIC);
            byte[] signatureBytes = Base64.getDecoder().decode(signature);

            String signatureData = "WALLET_ACCESS:" + wallet.getId();
            return StringUtil.verifyECDSASig(publicKey, signatureData, signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }


    public static boolean verifyTransactionSignature(WalletDTO wallet, String signatureWithTimestamp, String operation) {
        try {
            String[] parts = signatureWithTimestamp.split(":", 2);
            if (parts.length != 2) {
                return false;
            }

            long timestamp = Long.parseLong(parts[0]);
            String encodedSignature = parts[1];

            // Check if the transaction signature has expired
            long currentTime = System.currentTimeMillis();
            if (currentTime - timestamp > TRANSACTION_SIGNATURE_VALIDITY) {
                return false;
            }

            PublicKey publicKey = (PublicKey) StringUtil.decodeKey(wallet.getPublicKeyEncoded(), StringUtil.KeyType.PUBLIC);
            byte[] signatureBytes = Base64.getDecoder().decode(encodedSignature);

            // Verify using the same format used for signing
            String signatureData = String.format("%s:%s:%d", operation, wallet.getId(), timestamp);
            return StringUtil.verifyECDSASig(publicKey, signatureData, signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateTransactionSignature(PrivateKey privateKey, String walletId, String operation) {
        try {
            long timestamp = System.currentTimeMillis();
            String signatureData = String.format("%s:%s:%d", operation, walletId, timestamp);
            byte[] signature = StringUtil.applyECDSASig(privateKey, signatureData);
            return timestamp + ":" + Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create transaction signature", e);
        }
    }
}
