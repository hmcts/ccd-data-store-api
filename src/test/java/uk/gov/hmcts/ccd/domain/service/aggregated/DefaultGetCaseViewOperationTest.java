package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.listevents.ListEventsOperation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

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
    private CaseAuditEventRepository auditEventRepository;

    @Mock
    private ListEventsOperation listEventsOperation;

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
    private List<AuditEvent> auditEvents;
    private AuditEvent event1;
    private AuditEvent event2;
    private CaseTabCollection caseTabCollection;
    private CaseType caseType;
    private CaseState caseState;

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

        event1 = new AuditEvent();
        event1.setSummary(EVENT_SUMMARY_1);
        event2 = new AuditEvent();
        event2.setSummary(EVENT_SUMMARY_2);
        auditEvents = Arrays.asList(event1, event2);
        doReturn(auditEvents).when(listEventsOperation).execute(caseDetails);

        doReturn(Boolean.TRUE).when(uidService).validateUID(CASE_REFERENCE);


        caseTabCollection = buildCaseTabCollection("dataTestField1", "dataTestField2");
        doReturn(caseTabCollection).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        caseType = new CaseType();
        caseType.setJurisdiction(new Jurisdiction());
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

        caseState = new CaseState();
        doReturn(caseState).when(caseTypeService).findState(caseType, STATE);

        defaultGetCaseViewOperation = new DefaultGetCaseViewOperation(getCaseOperation,
                                                                      listEventsOperation,
                                                                      uiDefinitionRepository,
                                                                      caseTypeService,
                                                                      eventTriggerService,
                                                                      uidService);
    }

    private Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        Lists.newArrayList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }

    @Test
    @DisplayName("should retrieve all authorised audit events")
    void shouldRetrieveAllAuthorisedAuditEvents() {
        Map<String, JsonNode> dataMap = buildData("dataTestField1", "dataTestField2");
        caseDetails.setData(dataMap);

        final CaseView caseView = defaultGetCaseViewOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(
            () -> verify(listEventsOperation).execute(caseDetails),
            () -> assertThat(caseView.getTabs(), arrayWithSize(1)),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(2)),
            () -> assertThat(caseView.getTabs()[0].getFields(), hasItemInArray(hasProperty("id", equalTo("dataTestField1")))),
            () -> assertThat(caseView.getTabs()[0].getFields(), hasItemInArray(hasProperty("id", equalTo("dataTestField2")))),
            () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_1)))),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_2))))
        );
    }

    @Test
    @DisplayName("should retrieve only the authorised audit events")
    void shouldRetrieveOnlyAuthorisedAuditEvents() {
        Map<String, JsonNode> dataMap = buildData("dataTestField2");
        caseDetails.setData(dataMap);

        final CaseView caseView = defaultGetCaseViewOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(
            () -> verify(listEventsOperation).execute(caseDetails),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(1)),
            () -> assertThat(caseView.getTabs()[0].getFields(), hasItemInArray(hasProperty("id", equalTo("dataTestField2")))),
            () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_1)))),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_2))))
        );
    }

    private CaseTabCollection buildCaseTabCollection(String... caseFieldIds) {
        CaseTabCollection caseTabCollection = new CaseTabCollection();
        caseTabCollection.setChannels(Arrays.asList());
        List<CaseTypeTab> tabs = newArrayList();
        CaseTypeTab tab = new CaseTypeTab();
        List<CaseTypeTabField> tabFields = newArrayList();
        newArrayList(caseFieldIds).forEach(caseFieldId -> {
            CaseTypeTabField tabField = new CaseTypeTabField();
            CaseField caseField = new CaseField();
            caseField.setId(caseFieldId);
            FieldType fieldType = new FieldType();
            fieldType.setId("YesOrNo");
            caseField.setFieldType(fieldType);
            tabField.setCaseField(caseField);
            tabFields.add(tabField);
        });
        tab.setTabFields(tabFields);
        tabs.add(tab);
        caseTabCollection.setTabs(tabs);
        return caseTabCollection;
    }
}
