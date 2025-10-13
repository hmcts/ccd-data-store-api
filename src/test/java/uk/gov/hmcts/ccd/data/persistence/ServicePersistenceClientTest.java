package uk.gov.hmcts.ccd.data.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedAuditEvent;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedEventDetails;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedSubmitEventResponse;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedUpdateSupplementaryDataResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServicePersistenceClientTest {

    private static final String JURISDICTION = "TEST_JURISDICTION";
    private static final String CASE_TYPE = "TestCaseType";
    private static final Long CASE_REFERENCE = 1234567890123456L;
    private static final String CASE_ID = "1";
    private static final String CASE_STATE = "CaseCreated";
    private static final URI SERVICE_URI = URI.create("http://test-service.com");
    private static final UUID IDEMPOTENCY_KEY = UUID.randomUUID();
    private static final String EVENT_ID = "testEvent";

    @Mock
    private ServicePersistenceAPI api;

    @Mock
    private PersistenceStrategyResolver resolver;

    @Mock
    private IdempotencyKeyHolder idempotencyKeyHolder;

    @InjectMocks
    private ServicePersistenceClient servicePersistenceClient;

    private CaseDetails casePointer;
    private CaseDetails caseDetails;
    private DecentralisedCaseEvent caseEvent;
    private DecentralisedCaseDetails decentralisedCaseDetails;
    private DecentralisedSubmitEventResponse submitEventResponse;

    @Before
    public void setUp() {
        casePointer = createCasePointer();
        caseDetails = createCaseDetails();
        caseEvent = createDecentralisedCaseEvent();
        decentralisedCaseDetails = createDecentralisedCaseDetails();
        submitEventResponse = createSubmitEventResponse();
    }

    private CaseDetails createCasePointer() {
        CaseDetails pointer = new CaseDetails();
        pointer.setId(CASE_ID);
        pointer.setReference(CASE_REFERENCE);
        pointer.setJurisdiction(JURISDICTION);
        pointer.setCaseTypeId(CASE_TYPE);
        pointer.setState(CASE_STATE);
        pointer.setVersion(1);
        pointer.setSecurityClassification(uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC);
        pointer.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        pointer.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        return pointer;
    }

    private CaseDetails createCaseDetails() {
        CaseDetails details = new CaseDetails();
        details.setId(CASE_ID);
        details.setReference(CASE_REFERENCE);
        details.setJurisdiction(JURISDICTION);
        details.setCaseTypeId(CASE_TYPE);
        details.setState(CASE_STATE);
        details.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        details.setVersion(1);
        details.setSecurityClassification(uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC);
        details.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        return details;
    }

    private DecentralisedCaseEvent createDecentralisedCaseEvent() {
        return DecentralisedCaseEvent.builder()
            .caseDetails(casePointer)
            .eventDetails(DecentralisedEventDetails.builder()
                .eventId(EVENT_ID)
                .build())
            .build();
    }

    private DecentralisedCaseDetails createDecentralisedCaseDetails() {
        DecentralisedCaseDetails details = new DecentralisedCaseDetails();
        details.setCaseDetails(caseDetails);
        details.setRevision(1L);
        return details;
    }

    private DecentralisedSubmitEventResponse createSubmitEventResponse() {
        DecentralisedSubmitEventResponse response = new DecentralisedSubmitEventResponse();
        response.setCaseDetails(decentralisedCaseDetails);
        return response;
    }

    @Test
    public void getCase_shouldReturnCaseDetailsWithInternalId() {
        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(api.getCases(SERVICE_URI, List.of(CASE_REFERENCE)))
            .thenReturn(List.of(decentralisedCaseDetails));

        CaseDetails result = servicePersistenceClient.getCase(casePointer);

        assertAll("Case details should be correctly populated",
            () -> assertThat(result.getId(), is(CASE_ID)),
            () -> assertThat(result.getReference(), is(CASE_REFERENCE)),
            () -> assertThat(result.getCaseTypeId(), is(CASE_TYPE)),
            () -> assertThat(result.getJurisdiction(), is(JURISDICTION))
        );
    }

    @Test
    public void getCase_shouldThrowCaseNotFoundException_whenNoCasesReturned() {
        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(api.getCases(SERVICE_URI, List.of(CASE_REFERENCE)))
            .thenReturn(List.of());

        assertThrows(
            CaseNotFoundException.class,
            () -> servicePersistenceClient.getCase(casePointer)
        );
    }

    @Test
    public void getCase_shouldThrowServiceException_whenReferenceDoesNotMatch() {
        CaseDetails mismatchedCaseDetails = createCaseDetails();
        mismatchedCaseDetails.setReference(9999999999999999L);

        DecentralisedCaseDetails mismatchedDetails = new DecentralisedCaseDetails();
        mismatchedDetails.setCaseDetails(mismatchedCaseDetails);
        mismatchedDetails.setRevision(1L);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(api.getCases(SERVICE_URI, List.of(CASE_REFERENCE)))
            .thenReturn(List.of(mismatchedDetails));

        assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.getCase(casePointer)
        );
    }

    @Test
    public void getCase_shouldThrowServiceException_whenCaseTypeDoesNotMatch() {
        CaseDetails mismatchedCaseDetails = createCaseDetails();
        mismatchedCaseDetails.setCaseTypeId("DifferentCaseType");

        DecentralisedCaseDetails mismatchedDetails = new DecentralisedCaseDetails();
        mismatchedDetails.setCaseDetails(mismatchedCaseDetails);
        mismatchedDetails.setRevision(1L);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(api.getCases(SERVICE_URI, List.of(CASE_REFERENCE)))
            .thenReturn(List.of(mismatchedDetails));

        assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.getCase(casePointer)
        );
    }

    @Test
    public void getCase_shouldThrowServiceException_whenJurisdictionDoesNotMatch() {
        CaseDetails mismatchedCaseDetails = createCaseDetails();
        mismatchedCaseDetails.setJurisdiction("DifferentJurisdiction");

        DecentralisedCaseDetails mismatchedDetails = new DecentralisedCaseDetails();
        mismatchedDetails.setCaseDetails(mismatchedCaseDetails);
        mismatchedDetails.setRevision(1L);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(api.getCases(SERVICE_URI, List.of(CASE_REFERENCE)))
            .thenReturn(List.of(mismatchedDetails));

        assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.getCase(casePointer)
        );
    }

    @Test
    public void getCase_shouldThrowServiceException_whenVersionMissing() {
        CaseDetails noVersionCaseDetails = createCaseDetails();
        noVersionCaseDetails.setVersion(null);

        DecentralisedCaseDetails detailsMissingVersion = new DecentralisedCaseDetails();
        detailsMissingVersion.setCaseDetails(noVersionCaseDetails);
        detailsMissingVersion.setRevision(1L);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(api.getCases(SERVICE_URI, List.of(CASE_REFERENCE)))
            .thenReturn(List.of(detailsMissingVersion));

        assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.getCase(casePointer)
        );
    }

    @Test
    public void getCase_shouldThrowServiceException_whenSecurityClassificationMissing() {
        CaseDetails noSecurityCaseDetails = createCaseDetails();
        noSecurityCaseDetails.setSecurityClassification(null);

        DecentralisedCaseDetails detailsMissingSecurity = new DecentralisedCaseDetails();
        detailsMissingSecurity.setCaseDetails(noSecurityCaseDetails);
        detailsMissingSecurity.setRevision(1L);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(api.getCases(SERVICE_URI, List.of(CASE_REFERENCE)))
            .thenReturn(List.of(detailsMissingSecurity));

        assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.getCase(casePointer)
        );
    }

    @Test
    public void createEvent_shouldReturnCaseDetailsWithInternalId() {
        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(submitEventResponse);

        DecentralisedCaseDetails result = servicePersistenceClient.createEvent(caseEvent);
        CaseDetails caseDetailsResult = result.getCaseDetails();

        assertAll("Case details should be correctly populated",
            () -> assertThat(caseDetailsResult.getId(), is(CASE_ID)),
            () -> assertThat(caseDetailsResult.getReference(), is(CASE_REFERENCE)),
            () -> assertThat(caseDetailsResult.getCaseTypeId(), is(CASE_TYPE)),
            () -> assertThat(caseDetailsResult.getJurisdiction(), is(JURISDICTION))
        );
    }

    @Test
    public void createEvent_shouldThrowIllegalStateException_whenNoIdempotencyKey() {
        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(null);

        assertThrows(
            IllegalStateException.class,
            () -> servicePersistenceClient.createEvent(caseEvent)
        );
    }

    @Test
    public void createEvent_shouldThrowApiException_whenResponseHasErrors() {
        DecentralisedSubmitEventResponse errorResponse = new DecentralisedSubmitEventResponse();
        errorResponse.setCaseDetails(decentralisedCaseDetails);
        errorResponse.setErrors(List.of("Validation error"));

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(errorResponse);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> servicePersistenceClient.createEvent(caseEvent)
        );

        assertThat(exception.getMessage(),
            is("Unable to proceed because there are one or more callback Errors or Warnings"));
    }

    @Test
    public void createEvent_shouldThrowApiException_whenResponseHasWarningsAndIgnoreWarningIsFalse() {
        DecentralisedSubmitEventResponse warningResponse = new DecentralisedSubmitEventResponse();
        warningResponse.setCaseDetails(decentralisedCaseDetails);
        warningResponse.setWarnings(List.of("Warning message"));
        warningResponse.setIgnoreWarning(false);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(warningResponse);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> servicePersistenceClient.createEvent(caseEvent)
        );

        assertThat(exception.getMessage(),
            is("Unable to proceed because there are one or more callback Errors or Warnings"));
    }

    @Test
    public void createEvent_shouldSucceed_whenResponseHasWarningsButIgnoreWarningIsTrue() {
        DecentralisedSubmitEventResponse warningResponse = new DecentralisedSubmitEventResponse();
        warningResponse.setCaseDetails(decentralisedCaseDetails);
        warningResponse.setWarnings(List.of("Warning message"));
        warningResponse.setIgnoreWarning(true);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(warningResponse);

        DecentralisedCaseDetails result = servicePersistenceClient.createEvent(caseEvent);

        assertThat(result.getCaseDetails().getId(), is(CASE_ID));
    }

    @Test
    public void createEvent_shouldThrowServiceException_whenResponseHasMismatchedCaseDetails() {
        CaseDetails mismatchedCaseDetails = createCaseDetails();
        mismatchedCaseDetails.setReference(9999999999999999L);

        DecentralisedCaseDetails mismatchedDecentralisedCaseDetails = new DecentralisedCaseDetails();
        mismatchedDecentralisedCaseDetails.setCaseDetails(mismatchedCaseDetails);
        mismatchedDecentralisedCaseDetails.setRevision(1L);

        DecentralisedSubmitEventResponse mismatchedResponse = new DecentralisedSubmitEventResponse();
        mismatchedResponse.setCaseDetails(mismatchedDecentralisedCaseDetails);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(mismatchedResponse);

        ServiceException exception = assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.createEvent(caseEvent)
        );

        assertThat(exception.getMessage(),
            is("Downstream service returned mismatched case details for case reference " + CASE_REFERENCE));
    }

    @Test
    public void createEvent_shouldThrowServiceException_whenResponseHasMismatchedJurisdiction() {
        CaseDetails mismatchedCaseDetails = createCaseDetails();
        mismatchedCaseDetails.setJurisdiction("DifferentJurisdiction");

        DecentralisedCaseDetails mismatchedDecentralisedCaseDetails = new DecentralisedCaseDetails();
        mismatchedDecentralisedCaseDetails.setCaseDetails(mismatchedCaseDetails);
        mismatchedDecentralisedCaseDetails.setRevision(1L);

        DecentralisedSubmitEventResponse mismatchedResponse = new DecentralisedSubmitEventResponse();
        mismatchedResponse.setCaseDetails(mismatchedDecentralisedCaseDetails);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(mismatchedResponse);

        ServiceException exception = assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.createEvent(caseEvent)
        );

        assertThat(exception.getMessage(),
            is("Downstream service returned mismatched case details for case reference " + CASE_REFERENCE));
    }

    @Test
    public void createEvent_shouldThrowServiceException_whenResponseHasMissingVersion() {
        CaseDetails noVersionCaseDetails = createCaseDetails();
        noVersionCaseDetails.setVersion(null);

        DecentralisedCaseDetails noVersionDecentralisedCaseDetails = new DecentralisedCaseDetails();
        noVersionDecentralisedCaseDetails.setCaseDetails(noVersionCaseDetails);
        noVersionDecentralisedCaseDetails.setRevision(1L);

        DecentralisedSubmitEventResponse responseMissingVersion = new DecentralisedSubmitEventResponse();
        responseMissingVersion.setCaseDetails(noVersionDecentralisedCaseDetails);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(responseMissingVersion);

        assertThrows(ServiceException.class, () -> servicePersistenceClient.createEvent(caseEvent));
    }

    @Test
    public void createEvent_shouldThrowServiceException_whenResponseHasMissingSecurityClassification() {
        CaseDetails noSecurityCaseDetails = createCaseDetails();
        noSecurityCaseDetails.setSecurityClassification(null);

        DecentralisedCaseDetails noSecurityDecentralisedCaseDetails = new DecentralisedCaseDetails();
        noSecurityDecentralisedCaseDetails.setCaseDetails(noSecurityCaseDetails);
        noSecurityDecentralisedCaseDetails.setRevision(1L);

        DecentralisedSubmitEventResponse responseMissingSecurity = new DecentralisedSubmitEventResponse();
        responseMissingSecurity.setCaseDetails(noSecurityDecentralisedCaseDetails);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(SERVICE_URI, IDEMPOTENCY_KEY.toString(), caseEvent))
            .thenReturn(responseMissingSecurity);

        assertThrows(ServiceException.class, () -> servicePersistenceClient.createEvent(caseEvent));
    }

    @Test
    public void getCaseHistory_shouldReturnAuditEvents() {
        AuditEvent expectedEvent = new AuditEvent();
        expectedEvent.setCaseTypeId(CASE_TYPE);
        DecentralisedAuditEvent auditEvent = new DecentralisedAuditEvent();
        auditEvent.setId(321L);
        auditEvent.setCaseReference(CASE_REFERENCE);
        auditEvent.setEvent(expectedEvent);

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistory(SERVICE_URI, CASE_REFERENCE))
            .thenReturn(List.of(auditEvent));

        List<AuditEvent> result = servicePersistenceClient.getCaseHistory(caseDetails);

        assertThat(result.size(), is(1));
        AuditEvent actualEvent = result.get(0);
        assertAll(
            () -> assertThat(actualEvent, is(expectedEvent)),
            () -> assertThat(actualEvent.getId(), is(auditEvent.getId())),
            () -> assertThat(actualEvent.getCaseDataId(), is(CASE_ID)),
            () -> assertThat(actualEvent.getCaseTypeId(), is(CASE_TYPE))
        );
    }

    @Test
    public void getCaseHistoryEvent_shouldReturnSingleAuditEvent() {
        Long eventId = 123L;
        AuditEvent expectedEvent = new AuditEvent();
        expectedEvent.setCaseTypeId(CASE_TYPE);
        DecentralisedAuditEvent auditEvent = new DecentralisedAuditEvent();
        auditEvent.setId(eventId);
        auditEvent.setCaseReference(CASE_REFERENCE);
        auditEvent.setEvent(expectedEvent);

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistoryEvent(SERVICE_URI, CASE_REFERENCE, eventId))
            .thenReturn(auditEvent);

        AuditEvent result = servicePersistenceClient.getCaseHistoryEvent(caseDetails, eventId);

        assertAll(
            () -> assertThat(result, is(expectedEvent)),
            () -> assertThat(result.getId(), is(eventId)),
            () -> assertThat(result.getCaseDataId(), is(CASE_ID)),
            () -> assertThat(result.getCaseTypeId(), is(CASE_TYPE))
        );
    }

    @Test
    public void getCaseHistory_shouldThrowServiceException_whenAuditEventCaseReferenceDiffers() {
        DecentralisedAuditEvent auditEvent = new DecentralisedAuditEvent();
        auditEvent.setCaseReference(9999999999999999L);
        auditEvent.setEvent(new AuditEvent());

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistory(SERVICE_URI, CASE_REFERENCE))
            .thenReturn(List.of(auditEvent));

        assertThrows(ServiceException.class, () -> servicePersistenceClient.getCaseHistory(caseDetails));
    }

    @Test
    public void getCaseHistory_shouldThrowServiceException_whenAuditEventCaseTypeDiffers() {
        AuditEvent event = new AuditEvent();
        event.setCaseTypeId("WrongCaseType");

        DecentralisedAuditEvent auditEvent = new DecentralisedAuditEvent();
        auditEvent.setCaseReference(CASE_REFERENCE);
        auditEvent.setEvent(event);

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistory(SERVICE_URI, CASE_REFERENCE))
            .thenReturn(List.of(auditEvent));

        assertThrows(ServiceException.class, () -> servicePersistenceClient.getCaseHistory(caseDetails));
    }

    @Test
    public void getCaseHistoryEvent_shouldThrowServiceException_whenAuditEventCaseReferenceDiffers() {
        Long eventId = 321L;
        DecentralisedAuditEvent auditEvent = new DecentralisedAuditEvent();
        auditEvent.setId(eventId);
        auditEvent.setCaseReference(9999999999999999L);
        AuditEvent event = new AuditEvent();
        event.setCaseTypeId(CASE_TYPE);
        auditEvent.setEvent(event);

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistoryEvent(SERVICE_URI, CASE_REFERENCE, eventId))
            .thenReturn(auditEvent);

        assertThrows(ServiceException.class,
            () -> servicePersistenceClient.getCaseHistoryEvent(caseDetails, eventId));
    }

    @Test
    public void getCaseHistoryEvent_shouldThrowServiceException_whenAuditEventCaseTypeDiffers() {
        Long eventId = 321L;
        AuditEvent event = new AuditEvent();
        event.setCaseTypeId("WrongCaseType");

        DecentralisedAuditEvent auditEvent = new DecentralisedAuditEvent();
        auditEvent.setId(eventId);
        auditEvent.setCaseReference(CASE_REFERENCE);
        auditEvent.setEvent(event);

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistoryEvent(SERVICE_URI, CASE_REFERENCE, eventId))
            .thenReturn(auditEvent);

        assertThrows(ServiceException.class,
            () -> servicePersistenceClient.getCaseHistoryEvent(caseDetails, eventId));
    }

    @Test
    public void updateSupplementaryData_shouldReturnSupplementaryData() {
        SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();
        DecentralisedUpdateSupplementaryDataResponse response = mock(
            DecentralisedUpdateSupplementaryDataResponse.class);
        JsonNode expectedData = null;

        when(resolver.resolveUriOrThrow(CASE_REFERENCE)).thenReturn(SERVICE_URI);
        when(api.updateSupplementaryData(SERVICE_URI, CASE_REFERENCE, request))
            .thenReturn(response);
        when(response.getSupplementaryData()).thenReturn(expectedData);

        JsonNode result = servicePersistenceClient.updateSupplementaryData(CASE_REFERENCE, request);

        assertThat(result, is(expectedData));
    }
}
