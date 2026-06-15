package uk.gov.hmcts.ccd.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public final class CallbackAllowlistPreflight {
    private static final String ALLOWED_HOSTS_KEY = "CCD_CALLBACK_ALLOWED_HOSTS";
    private static final String ALLOWED_HTTP_HOSTS_KEY = "CCD_CALLBACK_ALLOWED_HTTP_HOSTS";
    private static final String ALLOW_PRIVATE_HOSTS_KEY = "CCD_CALLBACK_ALLOW_PRIVATE_HOSTS";
    private static final String MISSING_ENTRY_MESSAGE = " missing ";

    private CallbackAllowlistPreflight() {
    }

    public static String resolveStubHost(String beftaStubBaseUrl,
                                         String beftaStubHost,
                                         String defaultStubHost) throws MalformedURLException {
        if (hasText(beftaStubBaseUrl)) {
            return parseUrlHost(beftaStubBaseUrl);
        }
        if (hasText(beftaStubHost)) {
            return beftaStubHost.trim();
        }
        return defaultStubHost;
    }

    public static List<String> requiredHosts(String beftaStubBaseUrl, String aacHost) throws MalformedURLException {
        List<String> requiredHosts = new ArrayList<>();
        if (hasText(beftaStubBaseUrl)) {
            requiredHosts.add(parseUrlHost(beftaStubBaseUrl));
        }
        if (hasText(aacHost)) {
            requiredHosts.add(aacHost.trim());
        }
        return requiredHosts;
    }

    public static List<String> findAllowlistIssues(List<String> requiredHosts,
                                                   String callbackAllowedHosts,
                                                   String callbackAllowedHttpHosts,
                                                   String callbackAllowPrivateHosts) {
        CallbackHostPatternMatcher.validateEntries(callbackAllowedHosts);
        CallbackHostPatternMatcher.validateEntries(callbackAllowedHttpHosts);
        CallbackHostPatternMatcher.validateEntries(callbackAllowPrivateHosts);

        List<String> issues = new ArrayList<>();

        List<String> missingFromAllowed = requiredHosts.stream()
            .filter(host -> !CallbackHostPatternMatcher.containsHost(host, callbackAllowedHosts))
            .toList();
        List<String> missingFromHttpAllowed = requiredHosts.stream()
            .filter(host -> !CallbackHostPatternMatcher.containsHost(host, callbackAllowedHttpHosts))
            .toList();
        List<String> missingFromPrivateAllowed = requiredHosts.stream()
            .filter(host -> !CallbackHostPatternMatcher.containsHost(host, callbackAllowPrivateHosts))
            .toList();

        if (!missingFromAllowed.isEmpty()) {
            issues.add(ALLOWED_HOSTS_KEY + MISSING_ENTRY_MESSAGE + missingFromAllowed);
        }
        if (!missingFromHttpAllowed.isEmpty()) {
            issues.add(ALLOWED_HTTP_HOSTS_KEY + MISSING_ENTRY_MESSAGE + missingFromHttpAllowed);
        }
        if (!missingFromPrivateAllowed.isEmpty()) {
            issues.add(ALLOW_PRIVATE_HOSTS_KEY + MISSING_ENTRY_MESSAGE + missingFromPrivateAllowed);
        }

        return issues;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String parseUrlHost(String urlValue) throws MalformedURLException {
        String normalizedValue = normaliseYamlScalar(urlValue);
        try {
            URI uri = new URI(normalizedValue);
            if (!hasText(uri.getScheme()) || !hasText(uri.getHost())) {
                throw new MalformedURLException("URL must include a scheme and host: " + normalizedValue);
            }
            return uri.getHost();
        } catch (URISyntaxException exception) {
            MalformedURLException malformedURLException =
                new MalformedURLException("Invalid URL: " + normalizedValue);
            malformedURLException.initCause(exception);
            throw malformedURLException;
        }
    }

    public static String normaliseYamlScalar(String value) {
        if (!hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
            || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
