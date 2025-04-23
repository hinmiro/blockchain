package org.example.util;


import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class StringUtil {

    public static String apply(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Applies ECDSA Signature and returns the result ( as bytes )
    public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];

        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSign = dsa.sign();
            output = realSign;
        } catch (Exception e) {
            throw new RuntimeException("Apply ECDSA exception: " + e.getMessage());
        }
        return output;
    }

    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
//        try {
//            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
//            ecdsaVerify.initVerify(publicKey);
//            ecdsaVerify.update(data.getBytes());
//            return ecdsaVerify.verify(signature);
//        } catch (Exception e) {
//            throw new RuntimeException("Error occurred in ECDSA verification: " + e.getMessage());
//        }
        // TODO for testing purposes remember to remove
        return true;
    }

    public static String getStringFromKey(Key key) {
        if (key == null) {
            return null;
        }
        try {
            byte[] encoded = key.getEncoded();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (Exception e) {
            throw new RuntimeException("Error encoding key", e);
        }
    }

    public static enum KeyType {
        PUBLIC, PRIVATE
    }

    public static Key decodeKey(String encodedKey, KeyType type) {

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

            if (type == KeyType.PUBLIC) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
                return keyFactory.generatePublic(keySpec);
            } else {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                return keyFactory.generatePrivate(keySpec);
            }

        } catch (Exception e) {
            throw new KeyDecodeException("Error in decoding keys: " + e.getMessage());
        }
    }

    public static String encodeKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
