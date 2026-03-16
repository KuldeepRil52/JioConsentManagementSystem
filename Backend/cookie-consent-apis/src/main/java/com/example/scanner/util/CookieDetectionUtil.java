package com.example.scanner.util;

import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class CookieDetectionUtil {

    private static final Logger log = LoggerFactory.getLogger(CookieDetectionUtil.class);

    public static final List<String> CONSENT_ACCEPT_SELECTORS = Arrays.asList(
            // Basic accept buttons
            "button:has-text('Accept')", "button:has-text('Accept All')", "button:has-text('ACCEPT ALL')",
            "button:has-text('I Accept')", "button:has-text('Agree')", "button:has-text('AGREE')",
            "[data-testid*='accept']", "[data-cy*='accept']", "button[id*='accept']",
            "button[class*='accept']", "[aria-label*='accept']",

            // Cookie specific
            "[id*='cookie-accept']", "[class*='cookie-accept']", ".accept-cookies",

            // Popular CMPs
            "#onetrust-accept-btn-handler", ".fc-primary-button",
            "#CybotCookiebotDialogBodyButtonAccept", ".qc-cmp-button",

            // Generic
            ".btn-accept", ".button-accept", ".consent-accept"
    );

    /**
     * Handle consent banners - CRITICAL for more cookies
     */
    public static boolean handleConsentBanners(Page page) {
        boolean consentHandled = false;

        try {
            log.debug("Starting consent banner detection");

            // Try to accept cookies
            for (String selector : CONSENT_ACCEPT_SELECTORS) {
                try {
                    if (page.locator(selector).count() > 0) {
                        log.debug("Found consent element: {}", selector);
                        page.locator(selector).first().click();
                        page.waitForTimeout(2000);
                        log.info("Successfully clicked consent button: {}", selector);
                        consentHandled = true;
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Failed to click selector '{}'", selector);
                }
            }

            if (!consentHandled) {
                try {
                    if (page.locator("button").count() > 0) {
                        int buttonCount = page.locator("button").count();
                        for (int i = 0; i < Math.min(buttonCount, 10); i++) {
                            try {
                                String buttonText = page.locator("button").nth(i).textContent();
                                if (buttonText != null &&
                                        (buttonText.toLowerCase().contains("accept") ||
                                                buttonText.toLowerCase().contains("agree") ||
                                                buttonText.toLowerCase().contains("allow"))) {
                                    page.locator("button").nth(i).click();
                                    page.waitForTimeout(1000);
                                    log.info("Clicked consent button by text: {}", buttonText);
                                    consentHandled = true;
                                    break;
                                }
                            } catch (Exception e) {
                                // Continue to next button
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Fallback consent handling failed");
                }
            }

        } catch (Exception e) {
            log.warn("Error during consent banner handling");
        }

        log.info("Consent banner handling result: {}", consentHandled);
        return consentHandled;
    }
}