package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLinkInfo;
import uk.gov.hmcts.ccd.domain.model.caselinking.GetLinkedCasesResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkRetrievalService;
import uk.gov.hmcts.ccd.domain.service.caselinking.GetLinkedCasesResponseCreator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseEventsResource;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.v2.external.controller.CaseController.buildCaseIds;

@DisplayName("CaseController")
class CaseControllerTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String START_RECORD_NUMBER = "2";
    private static final String INVALID_START_RECORD_NUMBER = "A";
    private static final String MAX_RETURN_RECORD_COUNT = "1";
    private static final String INVALID_MAX_RETURN_RECORD_COUNT = "A";
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

    @Mock
    private GetLinkedCasesResponseCreator getLinkedCasesResponseCreator;

    @Mock
    private CaseLinkRetrievalService caseLinkRetrievalService;

    @InjectMocks
    private CaseController caseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseDetails.getReference()).thenReturn(Long.valueOf(CASE_REFERENCE));

        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.of(caseDetails));
        when(createEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT)).thenReturn(caseDetails);
        when(createCaseOperation.createCaseDetails(CASE_TYPE_ID, CASE_DATA_CONTENT, IGNORE_WARNING))
            .thenReturn(caseDetails);
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
            final ResponseEntity<CaseResource> response =
                caseController.createEvent(CASE_REFERENCE, CASE_DATA_CONTENT);

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
            when(createEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT))
                .thenThrow(RuntimeException.class);

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

            final ResponseEntity<CaseResource> response = caseController.createCase(CASE_TYPE_ID,
                CASE_DATA_CONTENT,
                IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.CREATED)),
                () -> assertThat(response.getBody().getReference(), is(CASE_REFERENCE)),
                () -> assertThat(response.getBody().getLastStateModifiedOn(), is(stateModified))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(createCaseOperation.createCaseDetails(CASE_TYPE_ID, CASE_DATA_CONTENT, IGNORE_WARNING))
                .thenThrow(RuntimeException.class);

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

        private final ObjectMapper mapper = new ObjectMapper();

        @Test
        @DisplayName("should return 200 when supplementary data updated")
        void shouldUpdateSupplementaryData() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
            Map<String, Object> data = createResponseData();
            SupplementaryData supplementaryData = new SupplementaryData(data);
            when(supplementaryDataUpdateOperation.updateSupplementaryData(anyString(), anyObject()))
                .thenReturn(supplementaryData);

            final ResponseEntity<SupplementaryDataResource> response =
                caseController.updateCaseSupplementaryData(CASE_REFERENCE, createRequestDataOrgA());

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
            SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, request));
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
            SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest(new HashMap<>());

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, request));
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
            SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();

            assertThrows(BadRequestException.class,
                () -> caseController.updateCaseSupplementaryData(CASE_REFERENCE, request));
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
            Map<String, Object> childMap = (Map<String, Object>) response.get("orgs_assigned_users");
            assertTrue(childMap.containsKey(expectedKey));
            assertEquals(expectedValue, childMap.get(expectedKey));
        }

    }

    @Nested
    @DisplayName("GET /cases/{caseId}/events proxied by user")
    class GetEventsForCaseIdWithProxiedByUser {

        @Test
        @DisplayName("should return proxied by user details when exists")
        void shouldReturnProxiedByUserDetails() {
            AuditEvent auditEvent = createAuditEvent();
            List<AuditEvent> auditEvents = Lists.newArrayList(auditEvent);
            when(getEventsOperation.getEvents(CASE_REFERENCE)).thenReturn(auditEvents);

            final ResponseEntity<CaseEventsResource> response = caseController.getCaseEvents(CASE_REFERENCE);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getAuditEvents().size(), is(1)),
                () -> assertThat(response.getBody().getAuditEvents().get(0).getProxiedBy(), is("Proxied")),
                () -> assertThat(response.getBody().getAuditEvents().get(0)
                    .getProxiedByFirstName(), is("Proxied_First_Name")),
                () -> assertThat(response.getBody().getAuditEvents().get(0)
                    .getProxiedByLastName(), is("Proxied_Last_Name")),
                () -> assertThat(response.getBody().getAuditEvents().get(0).getUserId(), is("UserId")),
                () -> assertThat(response.getBody().getAuditEvents().get(0)
                    .getUserFirstName(), is("First_Name")),
                () -> assertThat(response.getBody().getAuditEvents().get(0)
                    .getUserLastName(), is("Last_Name"))
            );
        }

        private AuditEvent createAuditEvent() {
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setProxiedByFirstName("Proxied_First_Name");
            auditEvent.setProxiedByLastName("Proxied_Last_Name");
            auditEvent.setProxiedBy("Proxied");
            auditEvent.setUserId("UserId");
            auditEvent.setUserFirstName("First_Name");
            auditEvent.setUserLastName("Last_Name");
            return auditEvent;
        }
    }

    @Nested
    @DisplayName("GET /getLinkedCases/{caseReference}")
    class GetLinkedCases {

        // NB: equivalent test for startRecordNumber not required as defaulted using `@RequestParam`
        @ParameterizedTest(name = "should return 200 when case found, and maxRecordNumber parameter is not set: {0}")
        @NullAndEmptySource
        void linkedCaseFoundWhenMaxRecordNumberNotSet(String maxRecordNumber) {
            // GIVEN
            GetLinkedCasesResponse getLinkedCasesResponse = GetLinkedCasesResponse.builder()
                .hasMoreRecords(false)
                .linkedCases(List.of(CaseLinkInfo.builder().build()))
                .build();
            when(getLinkedCasesResponseCreator.createResponse(any(), eq(CASE_REFERENCE)))
                .thenReturn(getLinkedCasesResponse);

            // WHEN
            final ResponseEntity<GetLinkedCasesResponse> response = caseController.getLinkedCase(CASE_REFERENCE,
                "100", maxRecordNumber);

            // THEN
            assertThat(response.getStatusCode(), is(HttpStatus.OK));
            assertNotNull(response.getBody());

            assertCallToGetStandardLinkedCases(
                "100",
                "0" // i.e. default to zero to turn off limit
            );
        }

        @Test
        @DisplayName("should return 200 when case found with parameters")
        void linkedCaseFoundUsingOptionalParametersSet() {
            // GIVEN
            GetLinkedCasesResponse getLinkedCasesResponse = GetLinkedCasesResponse.builder()
                .hasMoreRecords(false)
                .linkedCases(List.of(CaseLinkInfo.builder().build()))
                .build();
            when(getLinkedCasesResponseCreator.createResponse(any(), eq(CASE_REFERENCE)))
                .thenReturn(getLinkedCasesResponse);

            // WHEN
            final ResponseEntity<GetLinkedCasesResponse> response =
                caseController.getLinkedCase(CASE_REFERENCE, START_RECORD_NUMBER, MAX_RETURN_RECORD_COUNT);

            // THEN
            assertThat(response.getStatusCode(), is(HttpStatus.OK));
            assertNotNull(response.getBody());

            assertCallToGetStandardLinkedCases(START_RECORD_NUMBER, MAX_RETURN_RECORD_COUNT);
        }

        @Test
        @DisplayName("should propagate CaseNotFoundException when case NOT found")
        void linkedCaseNotFound() {
            // GIVEN
            doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);

            // WHEN / THEN
            assertThrows(ResourceNotFoundException.class, () -> caseController.getLinkedCase(CASE_REFERENCE,
                START_RECORD_NUMBER, MAX_RETURN_RECORD_COUNT),
                V2.Error.CASE_NOT_FOUND);
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference is not supplied")
        void linkedCaseReferenceNotValid() {
            // WHEN / THEN
            assertThrows(BadRequestException.class, () -> caseController.getLinkedCase(null,
                null, null), "Case Reference not Supplied");
        }

        @Test
        @DisplayName("should propagate BadRequestException when Start Record Number is Non Numeric")
        void linkedCaseStartRecordNumberIsNonNumeric() {
            // WHEN / THEN
            assertThrows(BadRequestException.class,() -> caseController.getLinkedCase(CASE_REFERENCE,
                INVALID_START_RECORD_NUMBER, null),
                V2.Error.PARAM_NOT_NUM);
        }

        @Test
        @DisplayName("should propagate BadRequestException when Max Return Record Count is not valid")
        void linkedCaseMaxReturnRecordCountIsNonNumeric() {
            // WHEN / THEN
            assertThrows(BadRequestException.class, () -> caseController.getLinkedCase(CASE_REFERENCE,
                null, INVALID_MAX_RETURN_RECORD_COUNT),
                V2.Error.PARAM_NOT_NUM);
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            // GIVEN
            doThrow(RuntimeException.class).when(getCaseOperation).execute(CASE_REFERENCE);

            // WHEN / THEN
            assertThrows(Exception.class, () -> caseController.getLinkedCase(CASE_REFERENCE,
                START_RECORD_NUMBER, MAX_RETURN_RECORD_COUNT));
        }

        private void assertCallToGetStandardLinkedCases(String expectedStartRecordNumber,
                                                        String expectedMaxRecordsNumber) {

            ArgumentCaptor<Integer> startRecordNumberCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> maxRecordsNumberCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<String> caseReferenceCaptor = ArgumentCaptor.forClass(String.class);

            verify(caseLinkRetrievalService).getStandardLinkedCases(caseReferenceCaptor.capture(),
                startRecordNumberCaptor.capture(), maxRecordsNumberCaptor.capture());
            assertEquals(CASE_REFERENCE, caseReferenceCaptor.getValue());
            assertEquals(expectedStartRecordNumber, startRecordNumberCaptor.getValue().toString());
            assertEquals(expectedMaxRecordsNumber, maxRecordsNumberCaptor.getValue().toString());
        }
    }

    @Nested
    @DisplayName("GET /getLinkedCases/{caseReference} [AuditLog tests]")
    class GetLinkedCasesAuditLogTests {

        @DisplayName("List empty: should return empty string when empty list is passed")
        @Test
        void shouldReturnEmptyStringWhenEmptyListPassed() {
            assertEquals("", buildCaseIds("", createGetLinkedCasesResponse(0)));
        }

        @DisplayName("List one: should return simple string when single list item is passed")
        @Test
        void shouldReturnSimpleStringWhenSingleListItemPassed() {
            assertEquals("reference-0", buildCaseIds("reference-0", createGetLinkedCasesResponse(0)));
        }

        @DisplayName("List many: should return CSV string when many list items are passed")
        @Test
        void shouldReturnCsvStringWhenManyListItemsPassed() {
            assertEquals(
                "reference-0,reference-1,reference-2,reference-3",
                buildCaseIds("reference-0", createGetLinkedCasesResponse(3))
            );
        }

        @DisplayName("List too many: should return max CSV string when too many list items are passed")
        @Test
        void shouldReturnMaxCsvListWhenTooManyListItemsPassed() {

            // GIVEN
            String expectedOutput = "reference-0," + createGetLinkedCasesResponse(MAX_CASE_IDS_LIST - 1)
                .getLinkedCases().stream()
                .map(CaseLinkInfo::getCaseReference)
                .collect(Collectors.joining(","));

            // WHEN
            String output = buildCaseIds("reference-0", createGetLinkedCasesResponse(MAX_CASE_IDS_LIST + 1));

            // THEN
            assertEquals(expectedOutput, output);
        }

        @DisplayName("should not fail if response passed is null")
        @Test
        void shouldNotFailIfResponsePassedIsNull() {
            assertEquals("reference-0", buildCaseIds("reference-0", null));
        }

        private GetLinkedCasesResponse createGetLinkedCasesResponse(int numberRequired) {
            final List<CaseLinkInfo> caseLinkInfos = IntStream.rangeClosed(1, numberRequired)
                .mapToObj(num -> CaseLinkInfo.builder().caseReference("reference-" + num).build())
                .collect(Collectors.toList());

            return GetLinkedCasesResponse.builder().linkedCases(caseLinkInfos).build();
        }
    }

}
