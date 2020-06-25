package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataRequest;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.resource.CaseEventsResource;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@DisplayName("CaseController")
class CaseControllerTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "Grant";
    private static final Boolean IGNORE_WARNING = true;
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().build();

    @Mock
    private GetCaseOperation getCaseOperation;
    @Mock
    private CreateEventOperation createEventOperation;
    @Mock
    private CreateCaseOperation createCaseOperation;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private GetEventsOperation getEventsOperation;

    @Mock
    private SupplementaryDataOperation supplementaryDataOperation;

    @InjectMocks
    private CaseController caseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseDetails.getReference()).thenReturn(new Long(CASE_REFERENCE));

        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.of(caseDetails));
        when(createEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT)).thenReturn(caseDetails);
        when(createCaseOperation.createCaseDetails(CASE_TYPE_ID, CASE_DATA_CONTENT, IGNORE_WARNING)).thenReturn(caseDetails);
        List<AuditEvent> auditEvents = Lists.newArrayList(new AuditEvent(), new AuditEvent());
        when(getEventsOperation.getEvents(CASE_REFERENCE)).thenReturn(auditEvents);
    }

    @Nested
    @DisplayName("GET /cases/{caseId}")
    class GetCaseForId {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            final ResponseEntity<CaseResource> response = caseController.getCase(CASE_REFERENCE);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getReference(), is(CASE_REFERENCE))
            );
        }

        @Test
        @DisplayName("should propagate CaseNotFoundException when case NOT found")
        void caseNotFound() {
            when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.empty());

            assertThrows(CaseNotFoundException.class,
                () -> caseController.getCase(CASE_REFERENCE));
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.getCase(CASE_REFERENCE));
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getCaseOperation.execute(CASE_REFERENCE)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class,
                () -> caseController.getCase(CASE_REFERENCE));
        }
    }

    @Nested
    @DisplayName("POST /cases/{caseId}/events")
    class PostCaseEventDefinition {

        @Test
        @DisplayName("should return 201 when case event created")
        void caseEventCreated() {
            final ResponseEntity<CaseResource> response = caseController.createEvent(CASE_REFERENCE, CASE_DATA_CONTENT);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.CREATED)),
                () -> assertThat(response.getBody().getReference(), is(CASE_REFERENCE))
            );
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.createEvent(CASE_REFERENCE, CASE_DATA_CONTENT));
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(createEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class,
                () -> caseController.createEvent(CASE_REFERENCE, CASE_DATA_CONTENT));
        }
    }

    @Nested
    @DisplayName("POST /case-types/{caseTypeId}/cases")
    class PostCase {

        @Test
        @DisplayName("should return 201 when case created")
        void caseEventCreated() {
            LocalDateTime stateModified = LocalDateTime.now();
            when(caseDetails.getLastStateModifiedDate()).thenReturn(stateModified);

            final ResponseEntity<CaseResource> response = caseController.createCase(CASE_TYPE_ID, CASE_DATA_CONTENT, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.CREATED)),
                () -> assertThat(response.getBody().getReference(), is(CASE_REFERENCE)),
                () -> assertThat(response.getBody().getLastStateModifiedOn(), is(stateModified))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(createCaseOperation.createCaseDetails(CASE_TYPE_ID, CASE_DATA_CONTENT, IGNORE_WARNING)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class,
                () -> caseController.createCase(CASE_TYPE_ID, CASE_DATA_CONTENT, IGNORE_WARNING));
        }
    }

    @Nested
    @DisplayName("GET /cases/{caseId}/events")
    class GetEventsForCaseId {

        @Test
        @DisplayName("should return 200 when events found")
        void caseFound() {
            final ResponseEntity<CaseEventsResource> response = caseController.getCaseEvents(CASE_REFERENCE);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getAuditEvents().size(), is(2))
            );
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.getCaseEvents(CASE_REFERENCE));
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getEventsOperation.getEvents(CASE_REFERENCE)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class,
                () -> caseController.getCaseEvents(CASE_REFERENCE));
        }
    }


    @Nested
    @DisplayName("POST /cases/{caseId}/supplementary-data")
    class UpdateSupplementaryData {

        @Test
        @DisplayName("should return 200 when supplementary data updated")
        void shouldUpdateSupplementaryData() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> child = new HashMap<>();
            child.put("testchild", 123);
            data.put("test", child);
            SupplementaryData supplementaryData = new SupplementaryData(data);
            when(supplementaryDataOperation.updateSupplementaryData(anyString(), anyObject())).thenReturn(supplementaryData);

            final ResponseEntity<SupplementaryDataResource> response = caseController.updateCaseSupplementaryData(CASE_REFERENCE, new SupplementaryDataRequest());

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getSupplementaryData().getSupplementaryData().size(), equalTo(1)),
                () -> assertThat(response.getBody().getSupplementaryData().getSupplementaryData(), is(data))
            );
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, new SupplementaryDataRequest()));
        }

    }
}
