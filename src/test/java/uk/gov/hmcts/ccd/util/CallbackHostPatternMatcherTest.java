package uk.gov.hmcts.ccd.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallbackHostPatternMatcherTest {

    @Test
    void shouldMatchWildcard() {
        assertTrue(CallbackHostPatternMatcher.matches("any.host", "*"));
    }

    @Test
    void shouldMatchLegacyWildcardSubdomain() {
        assertTrue(CallbackHostPatternMatcher.matches("sub.allowed.example", "*.allowed.example"));
        assertFalse(CallbackHostPatternMatcher.matches("allowed.example", "*.allowed.example"));
    }

    @Test
    void shouldMatchRegexPattern() {
        assertTrue(CallbackHostPatternMatcher.matches("pr-123.preview.platform.hmcts.net",
            ".*\\.preview\\.platform\\.hmcts\\.net"));
        assertFalse(CallbackHostPatternMatcher.matches("preview.platform.hmcts.net",
            ".*\\.preview\\.platform\\.hmcts\\.net"));
    }

    @Test
    void shouldFallbackToLiteralEqualityForNonMatchingHost() {
        assertTrue(CallbackHostPatternMatcher.matches("literal.host", "literal.host"));
        assertFalse(CallbackHostPatternMatcher.matches("other.host", "literal.host"));
    }

    @Test
    void shouldRejectInvalidRegexLikeEntry() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> CallbackHostPatternMatcher.validateEntry("*demo.platform.hmcts.net"));
        assertTrue(exception.getMessage().contains("Invalid callback allowlist pattern"));
    }

    @Test
    void shouldCheckListAllowlist() {
        assertTrue(CallbackHostPatternMatcher.containsHost("service.preview.platform.hmcts.net",
            List.of("localhost", ".*\\.preview\\.platform\\.hmcts\\.net")));
        assertFalse(CallbackHostPatternMatcher.containsHost("service.demo.platform.hmcts.net",
            List.of("localhost", ".*\\.preview\\.platform\\.hmcts\\.net")));
    }

    @Test
    void shouldCheckRawCommaSeparatedAllowlist() {
        assertTrue(CallbackHostPatternMatcher.containsHost("service.demo.platform.hmcts.net",
            "localhost,.*\\.demo\\.platform\\.hmcts\\.net"));
        assertFalse(CallbackHostPatternMatcher.containsHost("service.other.platform.hmcts.net",
            "localhost,.*\\.demo\\.platform\\.hmcts\\.net"));
    }
}
