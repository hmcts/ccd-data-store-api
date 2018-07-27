package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTabCollectionBuilder.aCaseTabCollection;

class DefaultGetCaseViewOperationTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1111222233334444";
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
    private EventTriggerService eventTriggerService;

    @Mock
    private UIDService uidService;

    private DefaultGetCaseViewOperation defaultGetCaseViewOperation;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState(STATE);
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(JURISDICTION_ID,
                                                                          CASE_TYPE_ID,
                                                                          CASE_REFERENCE);

        final AuditEvent event1 = new AuditEvent();
        event1.setSummary(EVENT_SUMMARY_1);
        final AuditEvent event2 = new AuditEvent();
        event2.setSummary(EVENT_SUMMARY_2);
        final List<AuditEvent> auditEvents = asList(event1, event2);
        doReturn(auditEvents).when(getEventsOperation).getEvents(caseDetails);

        doReturn(Boolean.TRUE).when(uidService).validateUID(CASE_REFERENCE);

        final CaseTabCollection caseTabCollection = aCaseTabCollection()
            .withFieldIds("dataTestField1", "dataTestField2").build();
        doReturn(caseTabCollection).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        final CaseType caseType = new CaseType();
        Jurisdiction jurisdiction = new Jurisdiction();
        jurisdiction.setName(JURISDICTION_ID);
        caseType.setJurisdiction(jurisdiction);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

        doReturn(new CaseState()).when(caseTypeService).findState(caseType, STATE);

        defaultGetCaseViewOperation = new DefaultGetCaseViewOperation(getCaseOperation,
                                                                      getEventsOperation,
                                                                      uiDefinitionRepository,
                                                                      caseTypeService,
                                                                      eventTriggerService,
                                                                      uidService);
    }

    @Test
    @DisplayName("should retrieve all authorised audit events and tabs")
    void shouldRetrieveAllAuthorisedAuditEventsAndTabs() {
        Map<String, JsonNode> dataMap = buildData("dataTestField1", "dataTestField2");
        caseDetails.setData(dataMap);

        final CaseView caseView = defaultGetCaseViewOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(() -> verify(getEventsOperation).getEvents(caseDetails),
                  () -> assertThat(caseView.getTabs(), arrayWithSize(1)),
                  () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(2)),
                  () -> assertThat(caseView.getTabs()[0].getFields(),
                                   hasItemInArray(allOf(hasProperty("id", equalTo("dataTestField1")),
                                                        hasProperty("showCondition",
                                                                    equalTo("dataTestField1-fieldShowCondition"))))),
                  () -> assertThat(caseView.getTabs()[0].getFields(),
                                   hasItemInArray(allOf(hasProperty("id", equalTo("dataTestField2")),
                                                        hasProperty("showCondition",
                                                                    equalTo("dataTestField2-fieldShowCondition"))))),
                  () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                  () -> assertThat(caseView.getEvents(),
                                   hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_1)))),
                  () -> assertThat(caseView.getEvents(),
                                   hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_2))))
        );
    }

    @Test
    @DisplayName("should retrieve only the authorised audit events")
    void shouldRetrieveOnlyAuthorisedAuditEvents() {
        Map<String, JsonNode> dataMap = buildData("dataTestField2");
        caseDetails.setData(dataMap);

        final CaseView caseView = defaultGetCaseViewOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(() -> verify(getEventsOperation).getEvents(caseDetails),
                  () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(1)),
                  () -> assertThat(caseView.getTabs()[0].getFields(),
                                   hasItemInArray(hasProperty("id", equalTo("dataTestField2")))),
                  () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                  () -> assertThat(caseView.getEvents(),
                                   hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_1)))),
                  () -> assertThat(caseView.getEvents(),
                                   hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_2))))
        );
    }

    private Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = new HashMap<>();
        asList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }
}
