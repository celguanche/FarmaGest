package com.farmagest.service;

import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import java.util.Arrays;

/**
 * Self-describing PBKDF2 hashes; passwords are never stored as plain text.
 */
final class PasswordHasher {
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    static String hash(char[] password) {
        Checks.require(password != null && password.length >= 12 && password.length <= 1024, "La contraseña debe tener entre 12 y 1024 caracteres");
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    static boolean verify(char[] password, String encoded) {
        if (password == null || password.length > 1024 || encoded == null) return false;
        try {
            String[] p = encoded.split("\\$", -1);
            if (p.length != 4 || !p[0].equals("pbkdf2-sha256")) return false;
            int iterations = Integer.parseInt(p[1]);
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(p[2]), expected = Base64.getDecoder().decode(p[3]);
            if (salt.length != 16 || expected.length != 32) return false;
            byte[] actual = derive(password, salt, iterations);
            try {
                return MessageDigest.isEqual(actual, expected);
            } finally {
                Arrays.fill(actual, (byte) 0);
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        } finally {
            spec.clearPassword();
        }
    }
}
