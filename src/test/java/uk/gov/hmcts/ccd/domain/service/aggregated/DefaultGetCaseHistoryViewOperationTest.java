package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTabCollectionBuilder.newCaseTabCollection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

class DefaultGetCaseHistoryViewOperationTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1111222233334444";
    private static final Long EVENT_ID = 100L;
    private static final String EVENT_SUMMARY_1 = "some summary";
    private static final String EVENT_SUMMARY_2 = "Another summary";
    private static final String STATE = "Plop";

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private GetEventsOperation getEventsOperation;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private UIDService uidService;

    @Mock
    private CompoundFieldOrderService compoundFieldOrderService;

    @Mock
    private FieldProcessorService fieldProcessorService;

    @InjectMocks
    private DefaultGetCaseHistoryViewOperation defaultGetCaseHistoryViewOperation;

    private CaseDetails caseDetails;
    private AuditEvent event1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState(STATE);
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);

        event1 = new AuditEvent();
        event1.setSummary(EVENT_SUMMARY_1);
        AuditEvent event2 = new AuditEvent();
        event2.setSummary(EVENT_SUMMARY_2);
        List<AuditEvent> auditEvents = asList(event1, event2);
        doReturn(auditEvents).when(getEventsOperation).getEvents(caseDetails);

        doReturn(Boolean.TRUE).when(uidService).validateUID(CASE_REFERENCE);


        CaseTabCollection caseTabCollection = newCaseTabCollection().withFieldIds("dataTestField1",
                                                                                  "dataTestField2").build();
        doReturn(caseTabCollection).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        CaseType caseType = new CaseType();
        Jurisdiction jurisdiction = new Jurisdiction();
        jurisdiction.setName(JURISDICTION_ID);
        caseType.setJurisdiction(jurisdiction);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

        CaseState caseState = new CaseState();
        doReturn(caseState).when(caseTypeService).findState(caseType, STATE);

        doAnswer(invocation -> invocation.getArgument(0)).when(fieldProcessorService).processCaseViewField(any());
    }

    @Test
    @DisplayName("should retrieve historic case data for the event")
    void shouldRetrieveCaseHistoryForTheEvent() {
        Map<String, JsonNode> dataMap = buildData("dataTestField2");
        caseDetails.setData(dataMap);
        event1.setData(dataMap);
        doReturn(Optional.of(event1)).when(getEventsOperation).getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);

        CaseHistoryView caseHistoryView = defaultGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID);

        assertAll(() -> verify(getEventsOperation).getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID),
                  () -> assertThat(caseHistoryView.getTabs()[0].getFields(), arrayWithSize(1)),
                  () -> assertThat(caseHistoryView.getTabs()[0].getFields(),
                hasItemInArray(hasProperty("id", equalTo("dataTestField2")))),
                  () -> assertThat(caseHistoryView.getEvent(), hasProperty("summary", equalTo(EVENT_SUMMARY_1))),
                  () -> assertThat(caseHistoryView.getCaseType().getJurisdiction().getName(),
                                   equalTo(JURISDICTION_ID)));
    }

    @Test
    @DisplayName("should throw exception when case reference is invalid")
    void shouldThrowExceptionWhenCaseRefIsInvalid() {
        doReturn(false).when(uidService).validateUID(CASE_REFERENCE);

        assertThrows(BadRequestException.class,
            () -> defaultGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID));
    }

    @Test
    @DisplayName("should throw exception when case is not found")
    void shouldThrowExceptionWhenCaseIsNotFound() {
        doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);

        assertThrows(ResourceNotFoundException.class,
            () -> defaultGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID));
    }

    @Test
    @DisplayName("should throw exception when no event found")
    void shouldThrowExceptionWhenNoEventFound() {
        doReturn(Optional.empty()).when(getEventsOperation).getEvent(JURISDICTION_ID, CASE_TYPE_ID, EVENT_ID);

        assertThrows(ResourceNotFoundException.class,
            () -> defaultGetCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID));
    }

    private Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = new HashMap<>();
        asList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }
}
