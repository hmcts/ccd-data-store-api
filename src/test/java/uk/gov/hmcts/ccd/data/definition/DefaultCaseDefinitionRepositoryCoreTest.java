package uk.gov.hmcts.ccd.data.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackUrlValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCaseDefinitionRepositoryCoreTest {

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
    @SuppressWarnings("java:S1874") // Intentional: verify behavior of deprecated path until removal.
    void shouldGetCaseTypesForJurisdictionAndValidateCallbacks() {
        when(applicationParams.jurisdictionCaseTypesDefURL("J1")).thenReturn("http://localhost/j1");
        CaseTypeDefinition ct = new CaseTypeDefinition();
        ct.setCaseFieldDefinitions(List.of(new CaseFieldDefinition()));
        ct.setCallbackGetCaseUrl("https://localhost/get");
        CaseEventDefinition event = new CaseEventDefinition();
        event.setCallBackURLAboutToStartEvent("https://localhost/start");
        ct.setEvents(List.of(event));
        when(definitionStoreClient.invokeGetRequest("http://localhost/j1", CaseTypeDefinition[].class))
            .thenReturn(new ResponseEntity<>(new CaseTypeDefinition[] {ct}, HttpStatus.OK));

        List<CaseTypeDefinition> result = subject.getCaseTypesForJurisdiction("J1");

        assertEquals(1, result.size());
        verify(callbackUrlValidator).validateCallbackUrl("https://localhost/get");
        verify(callbackUrlValidator).validateCallbackUrl("https://localhost/start");
    }

    @Test
    void shouldResolveCallbackUrlPlaceholderBeforeValidationForCaseTypeAndEvent() {
        System.setProperty("UNIT_TEST_CALLBACK_BASE_URL", "https://stub.example.test");
        try {
            when(applicationParams.caseTypeDefURL("CT1")).thenReturn("http://localhost/ct1");

            CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
            caseTypeDefinition.setCaseFieldDefinitions(List.of());
            caseTypeDefinition.setCallbackGetCaseUrl(
                "${UNIT_TEST_CALLBACK_BASE_URL}/callback_get_case_injectedData"
            );
            CaseEventDefinition eventDefinition = new CaseEventDefinition();
            eventDefinition.setCallBackURLAboutToSubmitEvent("${UNIT_TEST_CALLBACK_BASE_URL}/event-callback");
            caseTypeDefinition.setEvents(List.of(eventDefinition));

            when(definitionStoreClient.invokeGetRequest("http://localhost/ct1", CaseTypeDefinition.class))
                .thenReturn(new ResponseEntity<>(caseTypeDefinition, HttpStatus.OK));

            CaseTypeDefinition result = subject.getCaseType("CT1");

            assertEquals("https://stub.example.test/callback_get_case_injectedData", result.getCallbackGetCaseUrl());
            assertEquals("https://stub.example.test/event-callback",
                result.getEvents().get(0).getCallBackURLAboutToSubmitEvent());
            verify(callbackUrlValidator)
                .validateCallbackUrl("https://stub.example.test/callback_get_case_injectedData");
            verify(callbackUrlValidator).validateCallbackUrl("https://stub.example.test/event-callback");
        } finally {
            System.clearProperty("UNIT_TEST_CALLBACK_BASE_URL");
        }
    }

    @Test
    @SuppressWarnings("java:S1874") // Intentional: verify behavior of deprecated path until removal.
    void shouldThrowNotFoundForCaseTypesForJurisdiction() {
        when(applicationParams.jurisdictionCaseTypesDefURL("J1")).thenReturn("http://localhost/j1");
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
            .when(definitionStoreClient).invokeGetRequest("http://localhost/j1", CaseTypeDefinition[].class);

        assertThrows(ResourceNotFoundException.class, () -> subject.getCaseTypesForJurisdiction("J1"));
    }

    @Test
    @SuppressWarnings("java:S1874") // Intentional: verify behavior of deprecated path until removal.
    void shouldThrowServiceExceptionForCaseTypesForJurisdiction() {
        when(applicationParams.jurisdictionCaseTypesDefURL("J1")).thenReturn("http://localhost/j1");
        doThrow(new RuntimeException("boom"))
            .when(definitionStoreClient).invokeGetRequest("http://localhost/j1", CaseTypeDefinition[].class);

        assertThrows(ServiceException.class, () -> subject.getCaseTypesForJurisdiction("J1"));
    }

    @Test
    void shouldGetBaseTypes() {
        when(applicationParams.baseTypesURL()).thenReturn("http://localhost/base");
        when(definitionStoreClient.invokeGetRequest("http://localhost/base", FieldTypeDefinition[].class))
            .thenReturn(new ResponseEntity<>(new FieldTypeDefinition[] {new FieldTypeDefinition()}, HttpStatus.OK));

        assertEquals(1, subject.getBaseTypes().size());
    }

    @Test
    void shouldThrowNotFoundForBaseTypes() {
        when(applicationParams.baseTypesURL()).thenReturn("http://localhost/base");
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
            .when(definitionStoreClient).invokeGetRequest("http://localhost/base", FieldTypeDefinition[].class);

        assertThrows(ResourceNotFoundException.class, subject::getBaseTypes);
    }

    @Test
    void shouldReturnNullWhenUserRoleClassificationNotFound() {
        when(applicationParams.userRoleClassification()).thenReturn("http://localhost/role");
        when(definitionStoreClient.invokeGetRequest(anyString(), eq(UserRole.class), anyMap()))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertNull(subject.getUserRoleClassifications("role-x"));
    }

    @Test
    void shouldThrowServiceExceptionWhenUserRoleClassificationFails() {
        when(applicationParams.userRoleClassification()).thenReturn("http://localhost/role");
        when(definitionStoreClient.invokeGetRequest(anyString(), eq(UserRole.class), anyMap()))
            .thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> subject.getUserRoleClassifications("role-x"));
    }

    @Test
    void shouldGetClassificationsForNonEmptyUserRoleList() {
        when(applicationParams.userRolesClassificationsURL()).thenReturn("http://localhost/roles");
        when(definitionStoreClient.invokeGetRequest(eq("http://localhost/roles"), eq(UserRole[].class), anyMap()))
            .thenReturn(new ResponseEntity<>(new UserRole[] {new UserRole()}, HttpStatus.OK));

        assertEquals(1, subject.getClassificationsForUserRoleList(List.of("a")).size());
    }

    @Test
    void shouldGetLatestVersion() {
        when(applicationParams.caseTypeLatestVersionUrl("CT1")).thenReturn("http://localhost/ct1/v");
        CaseTypeDefinitionVersion v = new CaseTypeDefinitionVersion();
        when(definitionStoreClient.invokeGetRequest("http://localhost/ct1/v", CaseTypeDefinitionVersion.class))
            .thenReturn(new ResponseEntity<>(v, HttpStatus.OK));

        assertNotNull(subject.getLatestVersion("CT1"));
    }

    @Test
    void shouldThrowNotFoundForLatestVersion() {
        when(applicationParams.caseTypeLatestVersionUrl("CT1")).thenReturn("http://localhost/ct1/v");
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
            .when(definitionStoreClient).invokeGetRequest("http://localhost/ct1/v", CaseTypeDefinitionVersion.class);

        assertThrows(ResourceNotFoundException.class, () -> subject.getLatestVersion("CT1"));
    }

    @Test
    void shouldThrowServiceExceptionForLatestVersion() {
        when(applicationParams.caseTypeLatestVersionUrl("CT1")).thenReturn("http://localhost/ct1/v");
        doThrow(new RuntimeException("boom"))
            .when(definitionStoreClient).invokeGetRequest("http://localhost/ct1/v", CaseTypeDefinitionVersion.class);

        assertThrows(ServiceException.class, () -> subject.getLatestVersion("CT1"));
    }

    @Test
    void shouldReturnNullJurisdictionWhenNotFound() {
        when(applicationParams.jurisdictionDefURL()).thenReturn("http://localhost/jurisdictions");
        when(definitionStoreClient.invokeGetRequest(anyString(), eq(JurisdictionDefinition[].class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertNull(subject.getJurisdiction("J1"));
    }

    @Test
    void shouldThrowServiceExceptionWhenJurisdictionLookupFails() {
        when(applicationParams.jurisdictionDefURL()).thenReturn("http://localhost/jurisdictions");
        when(definitionStoreClient.invokeGetRequest(anyString(), eq(JurisdictionDefinition[].class)))
            .thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, subject::getAllJurisdictionsFromDefinitionStore);
    }

    @Test
    void shouldReturnNullWhenCaseTypeBodyIsNull() {
        when(applicationParams.caseTypeDefURL("CT-NULL")).thenReturn("http://localhost/ct-null");
        when(definitionStoreClient.invokeGetRequest("http://localhost/ct-null", CaseTypeDefinition.class))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertNull(subject.getCaseType("CT-NULL"));
    }

    @Test
    void shouldThrowServiceExceptionForGetCaseTypeWhenNon404() {
        when(applicationParams.caseTypeDefURL("CT-ERR")).thenReturn("http://localhost/ct-err");
        when(definitionStoreClient.invokeGetRequest("http://localhost/ct-err", CaseTypeDefinition.class))
            .thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> subject.getCaseType("CT-ERR"));
    }

    @Test
    void shouldThrowServiceExceptionForBaseTypesWhenNon404() {
        when(applicationParams.baseTypesURL()).thenReturn("http://localhost/base");
        doThrow(new RuntimeException("boom"))
            .when(definitionStoreClient).invokeGetRequest("http://localhost/base", FieldTypeDefinition[].class);

        assertThrows(ServiceException.class, subject::getBaseTypes);
    }

    @Test
    void shouldReturnFirstJurisdictionWhenFound() {
        when(applicationParams.jurisdictionDefURL()).thenReturn("http://localhost/jurisdictions");
        JurisdictionDefinition j1 = new JurisdictionDefinition();
        j1.setId("J1");
        when(definitionStoreClient.invokeGetRequest(anyString(), eq(JurisdictionDefinition[].class)))
            .thenReturn(new ResponseEntity<>(new JurisdictionDefinition[] {j1}, HttpStatus.OK));

        JurisdictionDefinition result = subject.getJurisdiction("J1");
        assertNotNull(result);
        assertEquals("J1", result.getId());
    }

    @Test
    void shouldBuildEncodedUserRoleQueryParam() {
        when(applicationParams.userRoleClassification()).thenReturn("http://localhost/role");
        when(definitionStoreClient.invokeGetRequest(anyString(), eq(UserRole.class), anyMap()))
            .thenReturn(new ResponseEntity<>(new UserRole(), HttpStatus.OK));

        subject.getUserRoleClassifications("role-x");

        verify(definitionStoreClient).invokeGetRequest(eq("http://localhost/role"), eq(UserRole.class), anyMap());
    }

    @Test
    void shouldNoOpWhenValidatingNullCaseTypeDefinition() {
        ReflectionTestUtils.invokeMethod(subject, "validateCaseTypeCallbackUrls", new Object[] {null});
    }

    @Test
    void shouldResolvePlaceholderBufferAndValidateEventCallbacks() {
        System.setProperty("UNIT_TEST_CALLBACK_BASE_URL_2", "https://stub2.example.test");
        try {
            CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
            caseTypeDefinition.setCallbackGetCaseUrl("${UNIT_TEST_CALLBACK_BASE_URL_2}/get-case");
            CaseEventDefinition event = new CaseEventDefinition();
            event.setCallBackURLAboutToStartEvent("${UNIT_TEST_CALLBACK_BASE_URL_2}/about-to-start");
            caseTypeDefinition.setEvents(List.of(event));

            ReflectionTestUtils.invokeMethod(subject, "validateCaseTypeCallbackUrls", caseTypeDefinition);

            verify(callbackUrlValidator).validateCallbackUrl("https://stub2.example.test/get-case");
            verify(callbackUrlValidator).validateCallbackUrl("https://stub2.example.test/about-to-start");
            assertEquals("https://stub2.example.test/about-to-start", event.getCallBackURLAboutToStartEvent());
        } finally {
            System.clearProperty("UNIT_TEST_CALLBACK_BASE_URL_2");
        }
    }
}
