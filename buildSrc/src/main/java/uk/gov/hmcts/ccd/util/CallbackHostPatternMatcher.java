package uk.gov.hmcts.ccd.util;

import java.util.ArrayList;
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
        return splitRawAllowlist(rawAllowlist).stream()
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
            return matchesRegex(host, trimmedEntry);
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
            Pattern.compile(trimmedEntry, Pattern.CASE_INSENSITIVE);
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
        splitRawAllowlist(rawAllowlist).stream()
            .map(String::trim)
            .filter(CallbackHostPatternMatcher::hasText)
            .forEach(CallbackHostPatternMatcher::validateEntry);
    }

    public static List<String> splitRawAllowlist(String rawAllowlist) {
        List<String> entries = new ArrayList<>();
        StringBuilder currentEntry = new StringBuilder();
        boolean escapingComma = false;

        for (int i = 0; i < rawAllowlist.length(); i++) {
            char currentChar = rawAllowlist.charAt(i);
            if (escapingComma) {
                if (currentChar == ',') {
                    currentEntry.append(',');
                } else {
                    currentEntry.append('\\').append(currentChar);
                }
                escapingComma = false;
            } else if (currentChar == '\\') {
                escapingComma = true;
            } else if (currentChar == ',') {
                entries.add(currentEntry.toString());
                currentEntry.setLength(0);
            } else {
                currentEntry.append(currentChar);
            }
        }

        if (escapingComma) {
            currentEntry.append('\\');
        }

        entries.add(currentEntry.toString());
        return entries;
    }

    private static boolean matchesRegex(String host, String entry) {
        try {
            return Pattern.compile(entry, Pattern.CASE_INSENSITIVE).matcher(host).matches();
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid callback allowlist pattern: " + entry, ex);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isPlainHostname(String value) {
        return value.matches("(?i)^[a-z0-9.-]+$");
    }
}
