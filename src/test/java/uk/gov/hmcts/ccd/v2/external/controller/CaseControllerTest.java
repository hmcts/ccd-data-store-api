package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.resource.CaseEventsResource;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
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
    private SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;

    @Mock
    private SupplementaryDataUpdateRequestValidator requestValidator;

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

        private ObjectMapper mapper = new ObjectMapper();

        @Test
        @DisplayName("should return 200 when supplementary data updated")
        void shouldUpdateSupplementaryData() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
            Map<String, Object> data = createResponseData();
            SupplementaryData supplementaryData = new SupplementaryData(data);
            when(supplementaryDataUpdateOperation.updateSupplementaryData(anyString(), anyObject())).thenReturn(supplementaryData);

            final ResponseEntity<SupplementaryDataResource> response = caseController.updateCaseSupplementaryData(CASE_REFERENCE, createRequestDataOrgA());

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getResponse().size(), equalTo(1)),
                () -> assertThat(response.getBody().getResponse(), is(data))
            );
            validateResponseData(response.getBody().getResponse(), "organisationA", 32);
        }

        @Test
        @DisplayName("should propagate BadRequestException when supplementary data not valid")
        void invalidSupplementaryDataUpdateRequest() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, new SupplementaryDataUpdateRequest()));
        }

        @Test
        @DisplayName("should propagate BadRequestException when supplementary data null")
        void shouldThrowBadRequestExceptionWhenSupplementaryDataNull() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, null));
        }

        @Test
        @DisplayName("should propagate BadRequestException when supplementary data has empty operation data")
        void shouldThrowBadRequestExceptionWhenSupplementaryDataHasNoData() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, new SupplementaryDataUpdateRequest(new HashMap<>())));
        }

        @Test
        @DisplayName("should propagate BadRequestException when supplementary data has more than one nested levels")
        void shouldThrowBadRequestExceptionWhenSupplementaryDataHasNestedLevels() {
            doCallRealMethod().when(requestValidator).validate(any(SupplementaryDataUpdateRequest.class));
            SupplementaryDataUpdateRequest request = createRequestDataNested();
            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, request));
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, new SupplementaryDataUpdateRequest()));
        }

        private Map<String, Object> createResponseData() {
            String jsonRequest = "{\n"
                + "\t\"orgs_assigned_users\": {\n"
                + "\t\t\"organisationA\": 32\n"
                + "\t}\n"
                + "}";
            return convertResponseData(jsonRequest);
        }

        private Map<String, Object> convertResponseData(String jsonRequest) {
            Map<String, Object> requestData;
            try {
                requestData = mapper.readValue(jsonRequest, Map.class);
            } catch (JsonProcessingException e) {
                requestData = new HashMap<>();
            }
            return requestData;
        }

        private SupplementaryDataUpdateRequest createRequestDataOrgA() {
            String jsonRequest = "{\n"
                + "\t\"$set\": {\n"
                + "\t\t\"orgs_assigned_users.organisationA\": 32\n"
                + "\t}\n"
                + "}";
            return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
        }

        private SupplementaryDataUpdateRequest createRequestDataNested() {
            String jsonRequest = "{\n"
                + "\t\"$set\": {\n"
                + "\t\t\"orgs_assigned_users.organisationA.organisationB\": 32\n"
                + "\t}\n"
                + "}";
            return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
        }

        private Map<String, Map<String, Object>> convertData(String jsonRquest) {
            Map<String, Map<String, Object>> requestData;
            try {
                requestData = mapper.readValue(jsonRquest, Map.class);
            } catch (JsonProcessingException e) {
                requestData = new HashMap<>();
            }
            return requestData;
        }

        private void validateResponseData(Map<String, Object> response, String expectedKey, Object expectedValue) {
            Map<String, Object> childMap = (Map<String, Object> ) response.get("orgs_assigned_users");
            assertTrue(childMap.containsKey(expectedKey));
            assertEquals(expectedValue, childMap.get(expectedKey));
        }

    }
}
