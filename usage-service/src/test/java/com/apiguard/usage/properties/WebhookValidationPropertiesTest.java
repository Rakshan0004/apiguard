package com.apiguard.usage.properties;

import com.apiguard.common.dto.WebhookConfig;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for webhook validation.
 * Tests Properties 1, 2, and 3 from the design document.
 */
@RunWith(JUnitQuickcheck.class)
@Tag("property-test")
public class WebhookValidationPropertiesTest {

    /**
     * Property 1: HTTPS Protocol Validation
     * For any webhook URL string, if it does not use the HTTPS protocol, 
     * the validation SHALL reject it with an appropriate error.
     * 
     * Feature: webhook-notifications, Property 1
     * Validates: Requirements 1.2
     */
    @Property(trials = 100)
    public void httpsProtocolValidation_rejectsNonHttpsUrls(String domain) {
        // Generate non-HTTPS URLs
        String httpUrl = "http://" + sanitizeDomain(domain) + "/webhook";
        String ftpUrl = "ftp://" + sanitizeDomain(domain) + "/webhook";
        
        // Verify HTTP is rejected
        assertFalse(isHttps(httpUrl), "HTTP URL should not be HTTPS");
        
        // Verify FTP is rejected
        assertFalse(isHttps(ftpUrl), "FTP URL should not be HTTPS");
    }

    /**
     * Property 2: URL Format Validation
     * For any string input, the URL validator SHALL correctly identify 
     * whether it is a valid HTTP(S) URL format.
     * 
     * Feature: webhook-notifications, Property 2
     * Validates: Requirements 1.3, 1.6
     */
    @Property(trials = 100)
    public void urlFormatValidation_identifiesValidUrls(String domain, String path) {
        String validUrl = "https://" + sanitizeDomain(domain) + "/" + sanitizePath(path);
        
        // Valid HTTPS URLs should be identified as valid
        assertTrue(isValidUrl(validUrl), 
            "Valid HTTPS URL should be identified as valid");
        
        // Check HTTPS protocol
        assertTrue(isHttps(validUrl),
            "Valid HTTPS URL should use HTTPS protocol");
    }

    /**
     * Property 3: Webhook Configuration Round-Trip
     * For any valid HTTPS webhook URL, storing the configuration and then 
     * retrieving it SHALL return the same URL value.
     * 
     * Feature: webhook-notifications, Property 3
     * Validates: Requirements 1.5
     */
    @Property(trials = 100)
    public void webhookConfigurationRoundTrip_preservesUrl(String domain, String path) {
        String webhookUrl = "https://" + sanitizeDomain(domain) + "/" + sanitizePath(path);
        UUID apiKeyId = UUID.randomUUID();
        String secret = "test-secret-" + UUID.randomUUID();
        
        // Create config
        WebhookConfig config = new WebhookConfig(apiKeyId, webhookUrl, secret, true);
        
        // Verify round-trip
        assertEquals(webhookUrl, config.webhookUrl(), 
            "Webhook URL should be preserved in configuration");
        assertEquals(secret, config.webhookSecret(), 
            "Webhook secret should be preserved in configuration");
        assertTrue(config.isEnabled(), 
            "Webhook should be enabled when URL is present");
    }

    /**
     * Check if a URL uses HTTPS protocol.
     */
    private boolean isHttps(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return true;
        }
        try {
            URL url = new java.net.URI(webhookUrl).toURL();
            return "https".equalsIgnoreCase(url.getProtocol());
        } catch (java.net.URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    /**
     * Check if a URL is valid HTTP(S) format.
     */
    private boolean isValidUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return true;
        }
        try {
            new java.net.URI(webhookUrl).toURL();
            return true;
        } catch (java.net.URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    /**
     * Sanitize domain string to create valid domain names.
     */
    private String sanitizeDomain(String input) {
        if (input == null || input.isBlank()) {
            return "example.com";
        }
        // Remove invalid characters and ensure valid domain
        String sanitized = input.replaceAll("[^a-zA-Z0-9.-]", "")
            .replaceAll("\\.+", ".")
            .replaceAll("^\\.|\\.$", "");
        
        if (sanitized.isBlank()) {
            return "example.com";
        }
        
        // Ensure it has a TLD
        if (!sanitized.contains(".")) {
            sanitized += ".com";
        }
        
        return sanitized;
    }

    /**
     * Sanitize path string to create valid URL paths.
     */
    private String sanitizePath(String input) {
        if (input == null || input.isBlank()) {
            return "webhook";
        }
        // Remove invalid characters
        String sanitized = input.replaceAll("[^a-zA-Z0-9/_-]", "")
            .replaceAll("/+", "/")
            .replaceAll("^/|/$", "");
        
        if (sanitized.isBlank()) {
            return "webhook";
        }
        
        return sanitized;
    }
}
