package com.apiguard.usage.properties;

import com.apiguard.usage.util.HmacSignatureGenerator;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for webhook security (HMAC signatures).
 * Tests Properties 12, 13, and 14 from the design document.
 */
@RunWith(JUnitQuickcheck.class)
@Tag("property-test")
public class WebhookSecurityPropertiesTest {

    /**
     * Property 12: HMAC Signature Determinism
     * For any payload, timestamp, and shared secret, generating the HMAC signature 
     * multiple times SHALL always produce the same signature value.
     * 
     * Feature: webhook-notifications, Property 12
     * Validates: Requirements 5.1, 5.2
     */
    @Property(trials = 100)
    public void hmacSignatureDeterminism_producesConsistentSignatures(String payload, long timestamp) {
        String secret = UUID.randomUUID().toString();
        String timestampStr = String.valueOf(Math.abs(timestamp));
        
        // Generate signature multiple times
        String signature1 = HmacSignatureGenerator.generateSignature(timestampStr, payload, secret);
        String signature2 = HmacSignatureGenerator.generateSignature(timestampStr, payload, secret);
        String signature3 = HmacSignatureGenerator.generateSignature(timestampStr, payload, secret);
        
        // All signatures should be identical
        assertEquals(signature1, signature2, 
            "HMAC signature should be deterministic (attempt 1 vs 2)");
        assertEquals(signature2, signature3, 
            "HMAC signature should be deterministic (attempt 2 vs 3)");
        assertEquals(signature1, signature3, 
            "HMAC signature should be deterministic (attempt 1 vs 3)");
    }

    /**
     * Property 13: HMAC Signature Verification
     * For any payload, timestamp, and shared secret, the signature generated 
     * SHALL successfully verify when checked with the same inputs.
     * 
     * Feature: webhook-notifications, Property 13
     * Validates: Requirements 5.5, 8.5
     */
    @Property(trials = 100)
    public void hmacSignatureVerification_validatesCorrectSignatures(String payload, long timestamp) {
        String secret = UUID.randomUUID().toString();
        String timestampStr = String.valueOf(Math.abs(timestamp));
        
        // Generate signature
        String signature = HmacSignatureGenerator.generateSignature(timestampStr, payload, secret);
        
        // Verify signature with same inputs
        boolean isValid = HmacSignatureGenerator.verifySignature(signature, timestampStr, payload, secret);
        
        assertTrue(isValid, 
            "Signature should verify successfully with correct inputs");
    }

    /**
     * Property 14: Invalid Signature Detection
     * For any webhook payload with a modified signature (that doesn't match the payload 
     * and timestamp), signature verification SHALL fail.
     * 
     * Feature: webhook-notifications, Property 14
     * Validates: Requirements 8.6
     */
    @Property(trials = 100)
    public void invalidSignatureDetection_rejectsModifiedSignatures(String payload, long timestamp) {
        String secret = UUID.randomUUID().toString();
        String timestampStr = String.valueOf(Math.abs(timestamp));
        
        // Generate valid signature
        String validSignature = HmacSignatureGenerator.generateSignature(timestampStr, payload, secret);
        
        // Modify the signature (flip a character)
        String modifiedSignature = modifySignature(validSignature);
        
        // Verify modified signature fails
        boolean isValid = HmacSignatureGenerator.verifySignature(modifiedSignature, timestampStr, payload, secret);
        
        assertFalse(isValid, 
            "Modified signature should fail verification");
    }

    /**
     * Additional test: Signature verification fails with modified payload
     */
    @Property(trials = 100)
    public void invalidSignatureDetection_rejectsModifiedPayload(String payload, long timestamp) {
        String secret = UUID.randomUUID().toString();
        String timestampStr = String.valueOf(Math.abs(timestamp));
        
        // Generate signature for original payload
        String signature = HmacSignatureGenerator.generateSignature(timestampStr, payload, secret);
        
        // Modify the payload
        String modifiedPayload = payload + "MODIFIED";
        
        // Verify signature fails with modified payload
        boolean isValid = HmacSignatureGenerator.verifySignature(signature, timestampStr, modifiedPayload, secret);
        
        assertFalse(isValid, 
            "Signature should fail verification when payload is modified");
    }

    /**
     * Additional test: Signature verification fails with modified timestamp
     */
    @Property(trials = 100)
    public void invalidSignatureDetection_rejectsModifiedTimestamp(String payload, long timestamp) {
        String secret = UUID.randomUUID().toString();
        String timestampStr = String.valueOf(Math.abs(timestamp));
        
        // Generate signature for original timestamp
        String signature = HmacSignatureGenerator.generateSignature(timestampStr, payload, secret);
        
        // Modify the timestamp
        String modifiedTimestamp = String.valueOf(Math.abs(timestamp) + 1000);
        
        // Verify signature fails with modified timestamp
        boolean isValid = HmacSignatureGenerator.verifySignature(signature, modifiedTimestamp, payload, secret);
        
        assertFalse(isValid, 
            "Signature should fail verification when timestamp is modified");
    }

    /**
     * Modify a signature string by flipping a character.
     */
    private String modifySignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return "modified";
        }
        
        char[] chars = signature.toCharArray();
        // Flip the first character
        if (chars[0] == 'a') {
            chars[0] = 'b';
        } else {
            chars[0] = 'a';
        }
        return new String(chars);
    }
}
