package com.apiguard.management.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class for validating webhook URLs.
 * Ensures URLs use HTTPS protocol and are properly formatted.
 */
public class WebhookUrlValidator {

    private static final int MAX_URL_LENGTH = 2048;
    private static final String HTTPS_PROTOCOL = "https";

    /**
     * Validate a webhook URL.
     *
     * @param webhookUrl URL to validate
     * @throws IllegalArgumentException if URL is invalid
     */
    public static void validate(String webhookUrl) {
        // Null or empty URLs are allowed (disables webhooks)
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        // Check length
        if (webhookUrl.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Webhook URL exceeds maximum length of " + MAX_URL_LENGTH + " characters");
        }

        // Parse URL to validate format
        URL url;
        try {
            url = new java.net.URI(webhookUrl).toURL();
        } catch (java.net.URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException("Invalid webhook URL format: " + e.getMessage());
        }

        // Validate HTTPS protocol
        if (!HTTPS_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            throw new IllegalArgumentException("Webhook URL must use HTTPS protocol");
        }
    }

    /**
     * Check if a URL uses HTTPS protocol.
     *
     * @param webhookUrl URL to check
     * @return true if URL is null/blank or uses HTTPS
     */
    public static boolean isHttps(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return true;
        }
        try {
            URL url = new java.net.URI(webhookUrl).toURL();
            return HTTPS_PROTOCOL.equalsIgnoreCase(url.getProtocol());
        } catch (java.net.URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    /**
     * Check if a URL is valid HTTP(S) format.
     *
     * @param webhookUrl URL to check
     * @return true if URL is valid HTTP(S) format
     */
    public static boolean isValidUrl(String webhookUrl) {
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
}
