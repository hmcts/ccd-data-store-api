package uk.gov.hmcts.ccd.data.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackUrlValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class DefaultCaseDefinitionRepositoryCallbackValidationTest {

    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private DefinitionStoreClient definitionStoreClient;
    @Mock
    private CallbackUrlValidator callbackUrlValidator;

    private DefaultCaseDefinitionRepository subject;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        subject = new DefaultCaseDefinitionRepository(applicationParams, definitionStoreClient, callbackUrlValidator);
    }

    @Test
    @DisplayName("should fail case type retrieval when callback host is not allowlisted")
    void shouldFailOnNonAllowlistedCallbackHost() {
        final String callbackUrl = "https://evil.example.com/callback";
        mockCaseTypeResponse(callbackUrl);
        doThrow(new CallbackException("Callback URL host is not allowlisted: evil.example.com"))
            .when(callbackUrlValidator).validateCallbackUrl(callbackUrl);

        ServiceException exception = assertThrows(ServiceException.class, () -> subject.getCaseType("CT1"));
        assertServiceExceptionCauseContains(exception, "host is not allowlisted");
    }

    @Test
    @DisplayName("should fail case type retrieval when callback scheme is not permitted")
    void shouldFailOnNonPermittedCallbackScheme() {
        final String callbackUrl = "http://evil.example.com/callback";
        mockCaseTypeResponse(callbackUrl);
        doThrow(new CallbackException("Callback URL scheme is not permitted: http"))
            .when(callbackUrlValidator).validateCallbackUrl(callbackUrl);

        ServiceException exception = assertThrows(ServiceException.class, () -> subject.getCaseType("CT2"));
        assertServiceExceptionCauseContains(exception, "scheme is not permitted");
    }

    @Test
    @DisplayName("should fail case type retrieval when callback resolves to private or local host")
    void shouldFailOnPrivateOrLocalCallbackHost() {
        final String callbackUrl = "https://localhost/callback";
        mockCaseTypeResponse(callbackUrl);
        doThrow(new CallbackException("Callback URL resolves to a private or local network address: localhost"))
            .when(callbackUrlValidator).validateCallbackUrl(callbackUrl);

        ServiceException exception = assertThrows(ServiceException.class, () -> subject.getCaseType("CT3"));
        assertServiceExceptionCauseContains(exception, "private or local network address");
    }

    @Test
    @DisplayName("should fail case type retrieval when callback URL includes embedded credentials")
    void shouldFailOnCallbackUrlWithEmbeddedCredentials() {
        final String callbackUrl = "https://user:pass@localhost/callback";
        mockCaseTypeResponse(callbackUrl);
        doThrow(new CallbackException("Callback URL must not include credentials: https://localhost/callback"))
            .when(callbackUrlValidator).validateCallbackUrl(callbackUrl);

        ServiceException exception = assertThrows(ServiceException.class, () -> subject.getCaseType("CT4"));
        assertServiceExceptionCauseContains(exception, "must not include credentials");
    }

    @Test
    @DisplayName("should validate event callback URLs during case type retrieval")
    void shouldValidateEventCallbackUrls() {
        CaseTypeDefinition caseTypeDefinition = buildCaseTypeWithCallback("https://localhost/callback");
        CaseEventDefinition eventDefinition = new CaseEventDefinition();
        eventDefinition.setCallBackURLAboutToSubmitEvent("https://evil.example.com/event-callback");
        caseTypeDefinition.setEvents(List.of(eventDefinition));
        when(definitionStoreClient.invokeGetRequest(nullable(String.class), eq(CaseTypeDefinition.class)))
            .thenReturn(new ResponseEntity<>(caseTypeDefinition, HttpStatus.OK));
        doThrow(new CallbackException("Callback URL host is not allowlisted: evil.example.com"))
            .when(callbackUrlValidator).validateCallbackUrl("https://evil.example.com/event-callback");

        ServiceException exception = assertThrows(ServiceException.class, () -> subject.getCaseType("CT5"));
        assertServiceExceptionCauseContains(exception, "host is not allowlisted");
    }

    private void mockCaseTypeResponse(String callbackUrl) {
        when(definitionStoreClient.invokeGetRequest(nullable(String.class), eq(CaseTypeDefinition.class)))
            .thenReturn(new ResponseEntity<>(buildCaseTypeWithCallback(callbackUrl), HttpStatus.OK));
    }

    private CaseTypeDefinition buildCaseTypeWithCallback(String callbackUrl) {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId("CT");
        Version version = new Version();
        version.setNumber(1);
        caseTypeDefinition.setVersion(version);
        caseTypeDefinition.setName("Case Type");
        caseTypeDefinition.setDescription("Case Type Desc");
        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId("PROBATE");
        jurisdictionDefinition.setName("Probate");
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        caseTypeDefinition.setCaseFieldDefinitions(new ArrayList<>());
        caseTypeDefinition.setCallbackGetCaseUrl(callbackUrl);
        return caseTypeDefinition;
    }

    private void assertServiceExceptionCauseContains(ServiceException exception, String expectedMessagePart) {
        assertTrue(exception.getCause() instanceof CallbackException);
        assertTrue(exception.getCause().getMessage().contains(expectedMessagePart));
    }
}
