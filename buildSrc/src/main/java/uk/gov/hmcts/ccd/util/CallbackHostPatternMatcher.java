package uk.gov.hmcts.ccd.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class CallbackHostPatternMatcher {
    private static final String ALLOWLIST_WILDCARD = "*";

    private CallbackHostPatternMatcher() {
    }

    public static boolean containsHost(String host, String rawAllowlist) {
        if (!hasText(host) || !hasText(rawAllowlist)) {
            return false;
        }
        return Arrays.stream(rawAllowlist.split(","))
            .map(String::trim)
            .anyMatch(entry -> matches(host, entry));
    }

    public static boolean containsHost(String host, List<String> allowlist) {
        if (!hasText(host) || allowlist == null) {
            return false;
        }
        return allowlist.stream()
            .filter(CallbackHostPatternMatcher::hasText)
            .map(String::trim)
            .anyMatch(entry -> matches(host, entry));
    }

    public static boolean matches(String host, String entry) {
        if (!hasText(host) || !hasText(entry)) {
            return false;
        }

        final String normalisedHost = host.toLowerCase(Locale.UK);
        final String trimmedEntry = entry.trim();
        final String normalisedEntry = trimmedEntry.toLowerCase(Locale.UK);

        if (ALLOWLIST_WILDCARD.equals(normalisedEntry)) {
            return true;
        }
        if (normalisedEntry.startsWith("*.")) {
            return normalisedHost.endsWith(normalisedEntry.substring(1));
        }
        if (!isPlainHostname(trimmedEntry)) {
            return matchesRegex(normalisedHost, normalisedEntry);
        }

        return normalisedHost.equals(normalisedEntry);
    }

    public static void validateEntry(String entry) {
        if (!hasText(entry)) {
            throw new IllegalArgumentException("Callback allowlist entry must not be blank");
        }
        String trimmedEntry = entry.trim();
        String normalisedEntry = trimmedEntry.toLowerCase(Locale.UK);

        if (ALLOWLIST_WILDCARD.equals(normalisedEntry) || normalisedEntry.startsWith("*.")
            || isPlainHostname(trimmedEntry)) {
            return;
        }

        try {
            Pattern.compile(normalisedEntry);
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid callback allowlist pattern: " + trimmedEntry, ex);
        }
    }

    public static void validateEntries(List<String> entries) {
        if (entries == null) {
            return;
        }
        entries.stream()
            .filter(CallbackHostPatternMatcher::hasText)
            .forEach(CallbackHostPatternMatcher::validateEntry);
    }

    public static void validateEntries(String rawAllowlist) {
        if (!hasText(rawAllowlist)) {
            return;
        }
        Arrays.stream(rawAllowlist.split(","))
            .map(String::trim)
            .filter(CallbackHostPatternMatcher::hasText)
            .forEach(CallbackHostPatternMatcher::validateEntry);
    }

    private static boolean matchesRegex(String normalisedHost, String normalisedEntry) {
        try {
            return Pattern.compile(normalisedEntry).matcher(normalisedHost).matches();
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid callback allowlist pattern: " + normalisedEntry, ex);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isPlainHostname(String value) {
        return value.matches("(?i)^[a-z0-9.-]+$");
    }
}
