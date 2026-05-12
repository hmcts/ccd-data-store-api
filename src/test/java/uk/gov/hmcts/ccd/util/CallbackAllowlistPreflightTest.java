package uk.gov.hmcts.ccd.util;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallbackAllowlistPreflightTest {

    @Test
    void shouldBuildRequiredHostsFromBeftaAndAac() throws Exception {
        List<String> requiredHosts = CallbackAllowlistPreflight.requiredHosts(
            "http://ccd-test-stubs-service-aat.service.core-compute-aat.internal/",
            "aac-manage-case-assignment-aat.service.core-compute-aat.internal");

        assertEquals(List.of(
            "ccd-test-stubs-service-aat.service.core-compute-aat.internal",
            "aac-manage-case-assignment-aat.service.core-compute-aat.internal"), requiredHosts);
    }

    @Test
    void shouldBuildRequiredHostsFromBeftaOnlyWhenAacMissing() throws Exception {
        List<String> requiredHosts = CallbackAllowlistPreflight.requiredHosts(
            "http://ccd-test-stubs-service-aat.service.core-compute-aat.internal/", " ");

        assertEquals(List.of("ccd-test-stubs-service-aat.service.core-compute-aat.internal"), requiredHosts);
    }

    @Test
    void shouldThrowForInvalidBeftaUrl() {
        assertThrows(MalformedURLException.class, () ->
            CallbackAllowlistPreflight.requiredHosts("not-a-url", "aac.service.internal"));
    }

    @Test
    void shouldPreferBeftaBaseUrlOverHostEnv() throws Exception {
        String resolvedHost = CallbackAllowlistPreflight.resolveStubHost(
            "http://resolved-from-base-url.internal/",
            "fallback-host.internal",
            "default-host.internal");

        assertEquals("resolved-from-base-url.internal", resolvedHost);
    }

    @Test
    void shouldFallbackToBeftaHostEnvWhenBaseUrlMissing() throws Exception {
        String resolvedHost = CallbackAllowlistPreflight.resolveStubHost(
            null,
            "fallback-host.internal",
            "default-host.internal");

        assertEquals("fallback-host.internal", resolvedHost);
    }

    @Test
    void shouldParseQuotedYamlUrlValue() throws Exception {
        String parsedHost = CallbackAllowlistPreflight.parseUrlHost("\"https://quoted-host.internal/\"");

        assertEquals("quoted-host.internal", parsedHost);
    }

    @Test
    void shouldReportMissingAllowlistEntries() {
        List<String> issues = CallbackAllowlistPreflight.findAllowlistIssues(
            List.of("stub.service.internal", "aac.service.internal"),
            "stub.service.internal",
            "stub.service.internal",
            "stub.service.internal");

        assertEquals(3, issues.size());
        assertTrue(issues.contains("CCD_CALLBACK_ALLOWED_HOSTS missing [aac.service.internal]"));
        assertTrue(issues.contains("CCD_CALLBACK_ALLOWED_HTTP_HOSTS missing [aac.service.internal]"));
        assertTrue(issues.contains("CCD_CALLBACK_ALLOW_PRIVATE_HOSTS missing [aac.service.internal]"));
    }

    @Test
    void shouldAcceptRegexMatchedAllowlistEntries() {
        List<String> issues = CallbackAllowlistPreflight.findAllowlistIssues(
            List.of("pr-123.preview.platform.hmcts.net"),
            ".*\\.preview\\.platform\\.hmcts\\.net",
            ".*\\.preview\\.platform\\.hmcts\\.net",
            ".*\\.preview\\.platform\\.hmcts\\.net");

        assertTrue(issues.isEmpty());
    }

    @Test
    void shouldRejectInvalidAllowlistPattern() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            CallbackAllowlistPreflight.findAllowlistIssues(
                List.of("pr-123.preview.platform.hmcts.net"),
                "*preview.platform.hmcts.net",
                ".*\\.preview\\.platform\\.hmcts\\.net",
                ".*\\.preview\\.platform\\.hmcts\\.net"));

        assertTrue(exception.getMessage().contains("Invalid callback allowlist pattern"));
    }
}
