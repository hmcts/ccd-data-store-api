package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.persistence.CasePointerRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenceStrategyResolverTest {

    private static final URI TEST_URI = URI.create("http://example.com");

    @Mock
    private CasePointerRepository casePointerRepository;

    private PersistenceStrategyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PersistenceStrategyResolver(casePointerRepository);
    }

    @Test
    void shouldHandleEmptyConfigurationGracefully() {
        assertDoesNotThrow(() -> resolver.setCaseTypeServiceUrls(Map.of()));
    }

    @Test
    void shouldStorePrefixesInLowercase() {
        resolver.setCaseTypeServiceUrls(Map.of("MyCase", TEST_URI));
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("MYCASE-Type");

        URI result = resolver.resolveUriOrThrow(caseDetails);

        assertEquals(TEST_URI, result);
    }

    @Test
    void shouldReturnFalseWhenCaseTypeIsNull() {
        resolver.setCaseTypeServiceUrls(Map.of("mycase", TEST_URI));
        CaseDetails caseDetails = new CaseDetails();

        assertFalse(resolver.isDecentralised(caseDetails));
    }

    @Test
    void shouldReturnFalseWhenCaseTypeIsBlank() {
        resolver.setCaseTypeServiceUrls(Map.of("mycase", TEST_URI));
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("   ");

        assertFalse(resolver.isDecentralised(caseDetails));
    }

    @Test
    void shouldThrowWhenMultiplePrefixesMatch() {
        resolver.setCaseTypeServiceUrls(Map.of(
            "pre", URI.create("http://one.test"),
            "prefix", URI.create("http://two.test")
        ));
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("Prefix-Case");

        assertThrows(IllegalStateException.class, () -> resolver.isDecentralised(caseDetails));
    }

    @Test
    void shouldReturnFalseWhenNoConfiguredPrefixMatches() {
        resolver.setCaseTypeServiceUrls(Map.of("configured", TEST_URI));
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("UnconfiguredCase");

        assertFalse(resolver.isDecentralised(caseDetails));
    }

    @Test
    void shouldResolveUriForCaseReferenceAndCacheCaseType() {
        Long reference = 1234567890123456L;
        resolver.setCaseTypeServiceUrls(Map.of("testcase", TEST_URI));
        when(casePointerRepository.findCaseTypeByReference(reference)).thenReturn("TestCase");

        URI firstResolution = resolver.resolveUriOrThrow(reference);
        URI secondResolution = resolver.resolveUriOrThrow(reference);

        assertEquals(TEST_URI, firstResolution);
        assertEquals(TEST_URI, secondResolution);
        verify(casePointerRepository, times(1)).findCaseTypeByReference(reference);
    }

    @Test
    void shouldThrowWhenNoRouteConfiguredForReference() {
        Long reference = 9876543210987654L;
        when(casePointerRepository.findCaseTypeByReference(reference)).thenReturn("UnknownType");

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
            () -> resolver.resolveUriOrThrow(reference));

        assertTrue(exception.getMessage().contains("UnknownType"));
    }

    @Test
    void shouldThrowWhenNoRouteConfiguredForCaseDetails() {
        resolver.setCaseTypeServiceUrls(Map.of("known", TEST_URI));
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("OtherType");

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
            () -> resolver.resolveUriOrThrow(caseDetails));

        assertTrue(exception.getMessage().contains("OtherType"));
    }
}
