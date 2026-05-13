package com.apiguard.management.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating secure webhook secrets.
 * Secrets are used for HMAC signature generation and verification.
 */
public class WebhookSecretGenerator {

    private static final int SECRET_BYTES = 32; // 256 bits
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a secure random webhook secret.
     * The secret is 32 bytes (256 bits) encoded as Base64.
     *
     * @return Base64-encoded secret string
     */
    public static String generateSecret() {
        byte[] randomBytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}
