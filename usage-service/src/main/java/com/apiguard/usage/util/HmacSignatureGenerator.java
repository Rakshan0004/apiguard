package com.apiguard.usage.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for generating and verifying HMAC-SHA256 signatures for webhooks.
 * Signatures are used to verify webhook authenticity.
 */
@Slf4j
public class HmacSignatureGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Generate HMAC-SHA256 signature for webhook payload.
     * The signature is computed over the concatenation of timestamp and payload.
     *
     * @param timestamp Unix timestamp in milliseconds
     * @param payload JSON payload string
     * @param secret Shared secret for HMAC
     * @return Hex-encoded signature string
     * @throws RuntimeException if signature generation fails
     */
    public static String generateSignature(String timestamp, String payload, String secret) {
        try {
            // Concatenate timestamp and payload
            String signatureInput = timestamp + payload;

            // Initialize HMAC with secret
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            hmac.init(secretKey);

            // Compute signature
            byte[] signatureBytes = hmac.doFinal(signatureInput.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            return bytesToHex(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate HMAC signature", e);
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    /**
     * Verify HMAC-SHA256 signature for webhook payload.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param receivedSignature Signature received in webhook
     * @param timestamp Unix timestamp in milliseconds
     * @param payload JSON payload string
     * @param secret Shared secret for HMAC
     * @return true if signature is valid
     */
    public static boolean verifySignature(String receivedSignature, String timestamp,
                                          String payload, String secret) {
        try {
            String expectedSignature = generateSignature(timestamp, payload, secret);

            // Use constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                receivedSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Failed to verify HMAC signature", e);
            return false;
        }
    }

    /**
     * Convert byte array to hex string.
     *
     * @param bytes Byte array
     * @return Hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
