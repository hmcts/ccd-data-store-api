package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

class CallbackUrlValidatorTest {

    @Mock
    private ApplicationParams applicationParams;

    private CallbackUrlValidator subject;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        subject = new CallbackUrlValidator(applicationParams);
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("localhost", "*.allowed.example"));
        when(applicationParams.getCallbackAllowedHttpHosts()).thenReturn(List.of("localhost"));
        when(applicationParams.getCallbackAllowPrivateHosts()).thenReturn(List.of("localhost"));
    }

    @Test
    void shouldRejectInvalidUri() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("not-a-uri"));
    }

    @Test
    void shouldRejectMalformedUriSyntax() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("https://[::1"));
    }

    @Test
    void shouldRejectMissingHost() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("https:///path"));
    }

    @Test
    void shouldRejectEmbeddedCredentials() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("https://user:pass@localhost/x"));
    }

    @Test
    void shouldRejectHostNotAllowlisted() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("https://evil.example.com/x"));
    }

    @Test
    void shouldRejectHttpForHostNotInHttpAllowlist() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("http://sub.allowed.example/x"));
    }

    @Test
    void shouldAllowHttpForHostInHttpAllowlist() {
        assertDoesNotThrow(() -> subject.validateCallbackUrl("http://localhost/x"));
    }

    @Test
    void shouldMatchWildcardSubdomainPattern() {
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(subject, "hostMatches",
            "sub.allowed.example", "*.allowed.example"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(subject, "hostMatches",
            "allowed.example", "*.allowed.example"));
    }

    @Test
    void shouldRejectPrivateHostWhenNotExplicitlyAllowed() {
        when(applicationParams.getCallbackAllowPrivateHosts()).thenReturn(List.of("internal-only.example"));
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("https://localhost/x"));
    }

    @Test
    void shouldSanitizeEmptyAndInvalidUrls() {
        assertTrue(subject.sanitizeUrl("").contains("<empty>"));
        assertTrue(subject.sanitizeUrl("://invalid").contains("<invalid-url>"));
    }

    @Test
    void shouldFailWhenHostCannotBeResolved() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("https://nonexistent.invalid/x"));
    }

    @Test
    void shouldAllowWhenHostAllowlistContainsWildcard() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(List.of("*"));
        when(applicationParams.getCallbackAllowedHttpHosts()).thenReturn(List.of("*"));
        when(applicationParams.getCallbackAllowPrivateHosts()).thenReturn(List.of("*"));

        assertDoesNotThrow(() -> subject.validateCallbackUrl("http://localhost/x"));
        assertDoesNotThrow(() -> subject.validateCallbackUrl("https://example.com/x"));
    }

    @Test
    void shouldRejectWhenAllowlistIsNull() {
        when(applicationParams.getCallbackAllowedHosts()).thenReturn(null);
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("https://localhost/x"));
    }

    @Test
    void shouldRejectWhenSchemeMissing() {
        assertThrows(CallbackException.class, () -> subject.validateCallbackUrl("localhost/path"));
    }

    @Test
    void shouldSanitizeValidUrlWithoutQueryAndCredentials() {
        String sanitized = subject.sanitizeUrl("https://user:pass@example.com:8443/path?q=1");
        assertTrue(sanitized.startsWith("https://example.com:8443/path"));
        assertFalse(sanitized.contains("user:pass"));
        assertFalse(sanitized.contains("?q=1"));
    }

    @Test
    void shouldClassifyIpv6UlaAsPrivate() throws Exception {
        InetAddress ula = InetAddress.getByAddress(new byte[] {
            (byte) 0xfd, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        });

        boolean isPrivate = (Boolean) ReflectionTestUtils.invokeMethod(subject, "isPrivateOrLocal", ula);
        assertTrue(isPrivate);
    }

    @Test
    void shouldClassifyMetadataEndpointAsPrivate() throws Exception {
        InetAddress metadata = InetAddress.getByName("169.254.169.254");

        boolean isPrivate = (Boolean) ReflectionTestUtils.invokeMethod(subject, "isPrivateOrLocal", metadata);
        assertTrue(isPrivate);
    }

    @Test
    void shouldReturnFalseWhenHostMissingOrAllowlistNull() {
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(subject, "isAllowedHost", "", List.of("*")));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(subject, "isAllowedHost", "example.com", null));
    }

    @Test
    void shouldMatchAllowlistWildcardDirectly() {
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(subject, "hostMatches", "any.host", "*"));
    }

    @Test
    void shouldResolvePublicAddressAsNonPrivate() {
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(subject, "resolvesToPrivateAddress", "8.8.8.8"));
    }

    @Test
    void shouldThrowWhenHostResolutionFailsInPrivateAddressCheck() {
        CallbackException exception = assertThrows(CallbackException.class,
            () -> ReflectionTestUtils.invokeMethod(subject, "resolvesToPrivateAddress", "%%%"));
        assertTrue(exception.getMessage().contains("Unable to resolve callback host"));
    }

}
