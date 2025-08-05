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
import static org.mockito.ArgumentMatchers.eq;
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
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setCaseTypeId(CASE_TYPE);
        caseDetails.setState(CASE_STATE);
        caseDetails.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        return caseDetails;
    }

    private CaseDetails createCaseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setCaseTypeId(CASE_TYPE);
        caseDetails.setState(CASE_STATE);
        caseDetails.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        return caseDetails;
    }

    private DecentralisedCaseEvent createDecentralisedCaseEvent() {
        return DecentralisedCaseEvent.builder()
            .caseDetails(casePointer)
            .build();
    }

    private DecentralisedCaseDetails createDecentralisedCaseDetails() {
        DecentralisedCaseDetails details = new DecentralisedCaseDetails();
        details.setCaseDetails(caseDetails);
        details.setVersion(1L);
        return details;
    }

    private DecentralisedSubmitEventResponse createSubmitEventResponse() {
        DecentralisedSubmitEventResponse response = new DecentralisedSubmitEventResponse();
        response.setCaseDetails(decentralisedCaseDetails);
        return response;
    }

    @Test
    public void getCase_shouldReturnCaseDetailsWithInternalId() {
        when(resolver.resolveUriOrThrow(CASE_REFERENCE)).thenReturn(SERVICE_URI);
        when(api.getCases(eq(SERVICE_URI), eq(List.of(CASE_REFERENCE))))
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
        when(resolver.resolveUriOrThrow(CASE_REFERENCE)).thenReturn(SERVICE_URI);
        when(api.getCases(eq(SERVICE_URI), eq(List.of(CASE_REFERENCE))))
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

        when(resolver.resolveUriOrThrow(CASE_REFERENCE)).thenReturn(SERVICE_URI);
        when(api.getCases(eq(SERVICE_URI), eq(List.of(CASE_REFERENCE))))
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

        when(resolver.resolveUriOrThrow(CASE_REFERENCE)).thenReturn(SERVICE_URI);
        when(api.getCases(eq(SERVICE_URI), eq(List.of(CASE_REFERENCE))))
            .thenReturn(List.of(mismatchedDetails));

        assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.getCase(casePointer)
        );
    }

    @Test
    public void createEvent_shouldReturnCaseDetailsWithInternalId() {
        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(eq(SERVICE_URI), eq(IDEMPOTENCY_KEY.toString()), eq(caseEvent)))
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
        when(api.submitEvent(eq(SERVICE_URI), eq(IDEMPOTENCY_KEY.toString()), eq(caseEvent)))
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
        when(api.submitEvent(eq(SERVICE_URI), eq(IDEMPOTENCY_KEY.toString()), eq(caseEvent)))
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
        when(api.submitEvent(eq(SERVICE_URI), eq(IDEMPOTENCY_KEY.toString()), eq(caseEvent)))
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
        mismatchedDecentralisedCaseDetails.setVersion(1L);

        DecentralisedSubmitEventResponse mismatchedResponse = new DecentralisedSubmitEventResponse();
        mismatchedResponse.setCaseDetails(mismatchedDecentralisedCaseDetails);

        when(resolver.resolveUriOrThrow(casePointer)).thenReturn(SERVICE_URI);
        when(idempotencyKeyHolder.getKey()).thenReturn(IDEMPOTENCY_KEY);
        when(api.submitEvent(eq(SERVICE_URI), eq(IDEMPOTENCY_KEY.toString()), eq(caseEvent)))
            .thenReturn(mismatchedResponse);

        ServiceException exception = assertThrows(
            ServiceException.class,
            () -> servicePersistenceClient.createEvent(caseEvent)
        );

        assertThat(exception.getMessage(),
            is("Downstream service returned mismatched case details for case reference " + CASE_REFERENCE));
    }

    @Test
    public void getCaseHistory_shouldReturnAuditEvents() {
        DecentralisedAuditEvent auditEvent = mock(DecentralisedAuditEvent.class);
        AuditEvent expectedEvent = new AuditEvent();

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistory(eq(SERVICE_URI), eq(CASE_REFERENCE)))
            .thenReturn(List.of(auditEvent));
        when(auditEvent.getEvent(CASE_ID)).thenReturn(expectedEvent);

        List<AuditEvent> result = servicePersistenceClient.getCaseHistory(caseDetails);

        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(expectedEvent));
    }

    @Test
    public void getCaseHistoryEvent_shouldReturnSingleAuditEvent() {
        Long eventId = 123L;
        DecentralisedAuditEvent auditEvent = mock(DecentralisedAuditEvent.class);
        AuditEvent expectedEvent = new AuditEvent();

        when(resolver.resolveUriOrThrow(caseDetails)).thenReturn(SERVICE_URI);
        when(api.getCaseHistoryEvent(eq(SERVICE_URI), eq(CASE_REFERENCE), eq(eventId)))
            .thenReturn(auditEvent);
        when(auditEvent.getEvent(CASE_ID)).thenReturn(expectedEvent);

        AuditEvent result = servicePersistenceClient.getCaseHistoryEvent(caseDetails, eventId);

        assertThat(result, is(expectedEvent));
    }

    @Test
    public void updateSupplementaryData_shouldReturnSupplementaryData() {
        SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();
        DecentralisedUpdateSupplementaryDataResponse response = mock(
            DecentralisedUpdateSupplementaryDataResponse.class);
        JsonNode expectedData = null;

        when(resolver.resolveUriOrThrow(CASE_REFERENCE)).thenReturn(SERVICE_URI);
        when(api.updateSupplementaryData(eq(SERVICE_URI), eq(CASE_REFERENCE), eq(request)))
            .thenReturn(response);
        when(response.getSupplementaryData()).thenReturn(expectedData);

        JsonNode result = servicePersistenceClient.updateSupplementaryData(CASE_REFERENCE, request);

        assertThat(result, is(expectedData));
    }
}
