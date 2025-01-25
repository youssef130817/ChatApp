package com.chat.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESEncryption {
    private static final String ALGORITHM = "AES";
    // Clé partagée de 16 caractères (128 bits)
    private static final String SHARED_KEY = "Ilisi32024AESKey";
    private final SecretKeySpec secretKey;

    public AESEncryption() {
        // Utilise la clé partagée pour créer la clé de chiffrement
        byte[] key = SHARED_KEY.getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(key, ALGORITHM);
    }

    public String encrypt(String message) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encryptedMessage) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
        return new String(decryptedBytes);
    }
}
