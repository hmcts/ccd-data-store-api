package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.ClassifiedGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.search.PaginatedSearchMetaDataOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.domain.service.stdapi.DocumentsOperation;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class CaseDetailsEndpointTest {

    private static final String UID = "1231";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_ID = "1234qwer5678tyui";
    private static final String EVENT_TRIGGER_ID = "updateEvent";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final CaseDataContent EVENT_DATA = newCaseDataContent().build();
    private static final Event EVENT = anEvent().build();
    private static final Map<String, JsonNode> DATA = new HashMap<>();
    private static final String TOKEN = "csdcsdcdscsdcsdcsdcd";

    static {
        EVENT_DATA.setEvent(EVENT);
        EVENT_DATA.setData(DATA);
        EVENT_DATA.setToken(TOKEN);
        EVENT_DATA.setIgnoreWarning(IGNORE_WARNING);
    }

    @Mock
    private ClassifiedGetCaseOperation classifiedGetCaseOperation;

    @Mock
    private CreateCaseOperation createCaseOperation;

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private DocumentsOperation documentsOperation;

    @Mock
    private SearchOperation searchOperation;

    @Mock
    private PaginatedSearchMetaDataOperation paginatedSearchMetaDataOperation;

    @Mock
    private FieldMapSanitizeOperation fieldMapSanitizeOperation;

    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @Mock
    private AppInsights appInsights;

    private CaseDetailsEndpoint endpoint;
    private Map<String,String> params = newHashMap();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        params = initParams("STATE");

        endpoint =
            new CaseDetailsEndpoint(classifiedGetCaseOperation,
                                    createCaseOperation,
                                    createEventOperation,
                                    startEventOperation,
                                    searchOperation,
                                    fieldMapSanitizeOperation,
                                    validateCaseFieldsOperation,
                                    documentsOperation,
                                    paginatedSearchMetaDataOperation,
                                    appInsights);
    }

    @Nested
    @DisplayName("findCaseDetailsForCaseworker()")
    class FindCaseDetailsForCaseworker {
        @Test
        @DisplayName("should return case retrieved by GetCaseOperation")
        void findCaseDetailsForCaseworker() {
            final Optional<CaseDetails> caseDetails = Optional.of(new CaseDetails());
            doReturn(caseDetails).when(classifiedGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_ID);

            final CaseDetails output = endpoint.findCaseDetailsForCaseworker(UID,
                                                                             JURISDICTION_ID,
                                                                             CASE_TYPE_ID,
                                                                             CASE_ID);

            assertThat(output, sameInstance(caseDetails.get()));
            verify(classifiedGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_ID);
        }

        @Test
        @DisplayName("should throw Not Found exception when no case found")
        void shouldThrowExceptionWhenNoCaseFound() {
            doReturn(Optional.empty()).when(classifiedGetCaseOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_ID);

            assertThrows(CaseNotFoundException.class,
                         () -> endpoint.findCaseDetailsForCaseworker(UID, JURISDICTION_ID, CASE_TYPE_ID, CASE_ID));
        }
    }

    @Test
    void shouldReturnStartEventTrigger_startEventForCaseworkerForCase() {
        final StartEventTrigger startEventTrigger = new StartEventTrigger();
        doReturn(startEventTrigger).when(startEventOperation).triggerStartForCase(UID,
                                                                                     JURISDICTION_ID,
                                                                                     CASE_TYPE_ID,
                                                                                     CASE_ID,
                                                                                     EVENT_TRIGGER_ID,
                                                                                     IGNORE_WARNING);

        final StartEventTrigger output = endpoint.startEventForCaseworker(UID,
                                                                          JURISDICTION_ID,
                                                                          CASE_TYPE_ID,
                                                                          CASE_ID,
                                                                          EVENT_TRIGGER_ID,
                                                                          IGNORE_WARNING);

        assertThat(output, sameInstance(startEventTrigger));
        verify(startEventOperation).triggerStartForCase(UID,
                                                        JURISDICTION_ID,
                                                        CASE_TYPE_ID,
                                                        CASE_ID,
                                                        EVENT_TRIGGER_ID,
                                                        IGNORE_WARNING);
    }

    @Test
    void shouldReturnStartEventTrigger_startEventForCaseworkerForCaseType() {
        final StartEventTrigger startEventTrigger = new StartEventTrigger();
        doReturn(startEventTrigger).when(startEventOperation).triggerStartForCaseType(UID,
                                                                                         JURISDICTION_ID,
                                                                                         CASE_TYPE_ID,
                                                                                         EVENT_TRIGGER_ID,
                                                                                         IGNORE_WARNING);

        final StartEventTrigger output = endpoint.startCaseForCaseworker(UID,
                                                                         JURISDICTION_ID,
                                                                         CASE_TYPE_ID,
                                                                         EVENT_TRIGGER_ID,
                                                                         IGNORE_WARNING);

        assertThat(output, sameInstance(startEventTrigger));
        verify(startEventOperation).triggerStartForCaseType(UID,
                                                            JURISDICTION_ID,
                                                            CASE_TYPE_ID,
                                                            EVENT_TRIGGER_ID,
                                                            IGNORE_WARNING);
    }

    @Test
    void createCaseForCaseWorker() {
        final CaseDetails toBeReturned = new CaseDetails();
        doReturn(toBeReturned).when(createCaseOperation).createCaseDetails(UID,
                                                                           JURISDICTION_ID,
                                                                           CASE_TYPE_ID,
                                                                           EVENT_DATA,
                                                                           IGNORE_WARNING);

        final CaseDetails output = endpoint.saveCaseDetailsForCaseWorker(UID,
                                                                         JURISDICTION_ID,
                                                                         CASE_TYPE_ID,
                                                                         IGNORE_WARNING,
                                                                         EVENT_DATA);

        assertAll(
            () -> assertThat(output, sameInstance(toBeReturned)),
            () -> verify(createCaseOperation).createCaseDetails(UID,
                                                                JURISDICTION_ID,
                                                                CASE_TYPE_ID,
                                                                EVENT_DATA,
                                                                IGNORE_WARNING)
        );
    }

    @Test
    void createCaseEventForCaseWorker() {
        final CaseDetails toBeReturned = new CaseDetails();
        doReturn(toBeReturned).when(createEventOperation).createCaseEvent(
            UID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_ID,
            EVENT,
            DATA,
            TOKEN,
            IGNORE_WARNING);

        final CaseDetails output = endpoint.createCaseEventForCaseWorker(
            UID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_ID,
            EVENT_DATA);

        assertAll(
            () -> assertThat(output, sameInstance(toBeReturned)),
            () -> verify(createEventOperation).createCaseEvent(
                UID,
                JURISDICTION_ID,
                CASE_TYPE_ID,
                CASE_ID,
                EVENT_DATA.getEvent(),
                EVENT_DATA.getData(),
                EVENT_DATA.getToken(),
                IGNORE_WARNING)
        );
    }

    @Test
    void validateCaseFieldsForCaseWorker() {
        final Map<String, JsonNode> toBeReturned = new HashMap<>();
        doReturn(toBeReturned).when(validateCaseFieldsOperation).validateCaseDetails(
            JURISDICTION_ID,
            CASE_TYPE_ID,
            EVENT,
            DATA);

        final Map<String, JsonNode> output = endpoint.validateCaseDetailsForCaseWorker(
            UID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            EVENT_DATA);

        assertAll(
            () -> assertThat(output, sameInstance(toBeReturned)),
            () -> verify(validateCaseFieldsOperation).validateCaseDetails(
                JURISDICTION_ID,
                CASE_TYPE_ID,
                EVENT_DATA.getEvent(),
                DATA)
        );
    }

    @Test
    void caseWorkersSearchCases() {
        final Map<String, String> sanitizedParams = initParams("StateS");
        given(fieldMapSanitizeOperation.execute(params)).willReturn(sanitizedParams);
        final ArgumentCaptor<MetaData> argument = ArgumentCaptor.forClass(MetaData.class);

        endpoint.searchCasesForCaseWorkers(JURISDICTION_ID, CASE_TYPE_ID, params);

        verify(fieldMapSanitizeOperation).execute(params);
        verify(searchOperation).execute(argument.capture(), eq(sanitizedParams));
        assertThat(argument.getValue().getCaseTypeId(), is(CASE_TYPE_ID));
        assertThat(argument.getValue().getJurisdiction(), is(JURISDICTION_ID));
        assertThat(argument.getValue().getState(), is(Optional.of("STATE")));
    }

    @Test
    @DisplayName("case worker search cases should throw error when metadata is unknown")
    void caseWorkersSearchCasesCaseUnknownMetadata() {

        params.put("notExisting1", "x");
        params.put("notExisting2", "y");
        params.put("state", "z");
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> endpoint.searchCasesForCaseWorkers(JURISDICTION_ID, "", params));

        assertThat(badRequestException.getMessage(), is("unknown metadata search parameters: notExisting2,notExisting1"));
    }

    @Test
    @DisplayName("case worker search cases should throw error when security classification is unknown")
    void caseWorkersSearchCasesCaseUnknownSecurityClassification() {

        params.put("security_classification", "XX");
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> endpoint.searchCasesForCaseWorkers(JURISDICTION_ID, "", params));

        assertThat(badRequestException.getMessage(), is("unknown security classification 'XX'"));
    }

    @Test
    @DisplayName("case worker search cases should throw error when sort direction is invalid")
    void caseWorkersSearchCasesCaseInvalidSortDirection() {

        params.put("sortDirection", "XX");
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
            () -> endpoint.searchCasesForCaseWorkers(JURISDICTION_ID, "", params));

        assertThat(badRequestException.getMessage(), is("Unknown sort direction: XX"));
    }

    @Test
    void citizenSearchCases() {
        final Map<String, String> sanitizedParams = initParams("StateC");
        given(fieldMapSanitizeOperation.execute(params)).willReturn(sanitizedParams);
        final ArgumentCaptor<MetaData> argument = ArgumentCaptor.forClass(MetaData.class);

        endpoint.searchCasesForCitizens(JURISDICTION_ID, CASE_TYPE_ID, params);

        verify(fieldMapSanitizeOperation).execute(params);
        verify(searchOperation).execute(argument.capture(), eq(sanitizedParams));
        assertThat(argument.getValue().getCaseTypeId(), is(CASE_TYPE_ID));
        assertThat(argument.getValue().getJurisdiction(), is(JURISDICTION_ID));
        assertThat(argument.getValue().getState(), is(Optional.of("STATE")));
    }

    private Map<String, String> initParams(final String state) {
        return new HashMap<String, String>() {{
            put("state", state);
        }};
    }

}
