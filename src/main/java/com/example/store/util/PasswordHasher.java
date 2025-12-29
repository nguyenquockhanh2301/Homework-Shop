package com.example.store.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class PasswordHasher {
    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LEN = 16; // bytes

    private final SecureRandom random = new SecureRandom();

    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LEN];
        random.nextBytes(salt);
        return salt;
    }

    public byte[] hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to hash password", e);
        }
    }

    public boolean verify(char[] password, byte[] salt, byte[] expectedHash) {
        byte[] hash = hash(password, salt);
        return constantTimeEquals(hash, expectedHash);
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public String paramsSummary() {
        return "PBKDF2-HMAC-SHA256 iterations=" + ITERATIONS + ", keyLength=" + KEY_LENGTH + ", salt=" + SALT_LEN + "B";
    }
}
