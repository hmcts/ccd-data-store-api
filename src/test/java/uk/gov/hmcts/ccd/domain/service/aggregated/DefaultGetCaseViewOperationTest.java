package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.CASE_HISTORY_VIEWER;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTabCollectionBuilder.newCaseTabCollection;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeTabBuilder.newCaseTab;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeTabFieldBuilder.newCaseTabField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class DefaultGetCaseViewOperationTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_REFERENCE = "1111222233334444";
    private static final String EVENT_SUMMARY_1 = "some summary";
    private static final String EVENT_SUMMARY_2 = "Another summary";
    private static final String STATE = "Plop";
    private static final String TITLE_DISPLAY = "titleDisplay";

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

    @Mock
    private ObjectMapperService objectMapperService;

    @Mock
    private CompoundFieldOrderService compoundFieldOrderService;

    @Mock
    private FieldProcessorService fieldProcessorService;

    @Spy
    @InjectMocks
    private DefaultGetCaseViewOperation defaultGetCaseViewOperation;

    private CaseDetails caseDetails;
    private List<AuditEvent> auditEvents;
    private AuditEvent event1;
    private AuditEvent event2;
    private CaseTabCollection caseTabCollection;
    private CaseType caseType;
    private CaseState caseState;
    private JsonNode eventsNode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState(STATE);
        Map<String, JsonNode> dataMap = buildData("dataTestField1", "dataTestField2");
        caseDetails.setData(dataMap);
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);

        event1 = new AuditEvent();
        event1.setSummary(EVENT_SUMMARY_1);
        event2 = new AuditEvent();
        event2.setSummary(EVENT_SUMMARY_2);
        auditEvents = asList(event1, event2);
        doReturn(auditEvents).when(getEventsOperation).getEvents(caseDetails);

        doReturn(eventsNode).when(objectMapperService).convertJsonNodeToMap(anyObject());

        doReturn(Boolean.TRUE).when(uidService).validateUID(CASE_REFERENCE);

        caseTabCollection = newCaseTabCollection().withFieldIds("dataTestField1", "dataTestField2").build();
        doReturn(caseTabCollection).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        caseType = new CaseType();
        Jurisdiction jurisdiction = new Jurisdiction();
        jurisdiction.setName(JURISDICTION_ID);
        caseType.setJurisdiction(jurisdiction);
        CaseField caseField = new CaseField();
        caseField.setId(MetaData.CaseField.CASE_TYPE.getReference());
        caseField.setMetadata(true);
        caseField.setFieldType(new FieldType());
        caseType.setCaseFields(singletonList(caseField));

        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

        caseState = new CaseState();
        caseState.setId(STATE);
        caseState.setTitleDisplay(TITLE_DISPLAY);
        doReturn(caseState).when(caseTypeService).findState(caseType, STATE);

        doAnswer(invocation -> invocation.getArgument(0)).when(fieldProcessorService).processCaseViewField(any());
    }

    @Nested
    @DisplayName("field of CaseHistoryViewer field type")
    class CaseHistoryViewer_FieldType {
        @Test
        @DisplayName("should hydrate case history viewer if CaseHistoryViewer field type present in tabs")
        void shouldHydrateCaseHistoryViewerIfFieldPresentInTabs() {
            caseTabCollection = newCaseTabCollection().withTab(newCaseTab()
                                                                   .withTabField(newCaseTabField()
                                                                                     .withCaseField(newCaseField()
                                                                                                        .withId(CASE_HISTORY_VIEWER)
                                                                                                        .withFieldType(aFieldType()
                                                                                                                           .withType(
                                                                                                                               CASE_HISTORY_VIEWER)
                                                                                                                           .build())
                                                                                                        .build())
                                                                                     .build())
                                                                   .build())
                                                      .build();
            doReturn(caseTabCollection).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);
            caseType.setCaseFields(singletonList(newCaseField().withId(CASE_HISTORY_VIEWER).withFieldType(aFieldType().withType(CASE_HISTORY_VIEWER).build()).build()));
            doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

            final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

            assertAll(() -> assertThat(caseView.getTabs(), arrayWithSize(1)),
                      () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(1)),
                      () -> assertThat(caseView.getTabs()[0].getFields()[0], hasProperty("id", equalTo(CASE_HISTORY_VIEWER))),
                      () -> assertThat(caseView.getTabs()[0].getFields()[0], hasProperty("value", equalTo(eventsNode))),
                      () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                      () -> assertThat(caseView.getEvents(),
                                       hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_1)))),
                      () -> assertThat(caseView.getEvents(),
                                       hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_2))))
            );
        }

        @Test
        @DisplayName("should not hydrate case history viewer if CaseHistoryViewer field type is not present in tabs")
        void shouldNotHydrateCaseHistoryViewerIfFieldIsNotPresentInTabs() {
            caseTabCollection = newCaseTabCollection().withTab(newCaseTab()
                                                                   .withTabField(newCaseTabField()
                                                                                     .withCaseField(newCaseField()
                                                                                                        .withId("NotACaseHistoryViewer")
                                                                                                        .withFieldType(aFieldType()
                                                                                                                           .withType(
                                                                                                                               "NotACaseHistoryViewer")
                                                                                                                           .build())
                                                                                                        .build())
                                                                                     .build())
                                                                   .build())
                                                      .build();
            doReturn(caseTabCollection).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);
            caseType.setCaseFields(singletonList(newCaseField()
                                                     .withId(CASE_HISTORY_VIEWER)
                                                     .withFieldType(aFieldType()
                                                                        .withType(CASE_HISTORY_VIEWER)
                                                                        .build())
                                                     .build()));
            doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

            final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

            assertAll(() -> assertThat(caseView.getTabs(), arrayWithSize(1)),
                      () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(0)),
                      () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                      () -> assertThat(caseView.getEvents(),
                                       hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_1)))),
                      () -> assertThat(caseView.getEvents(),
                                       hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_2))))
            );
        }
    }

    @Test
    @DisplayName("should call not-deprecated #execute(caseReference)")
    void shouldCallNotDeprecatedExecute() {
        final CaseView expectedCaseView = new CaseView();
        doReturn(expectedCaseView).when(defaultGetCaseViewOperation).execute(CASE_REFERENCE);

        final CaseView actualCaseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

        assertAll(
            () -> verify(defaultGetCaseViewOperation).execute(CASE_REFERENCE),
            () -> assertThat(actualCaseView, sameInstance(expectedCaseView))
        );
    }

    @Test
    @DisplayName("should retrieve all authorised audit events and tabs")
    void shouldRetrieveAllAuthorisedAuditEventsAndTabs() {
        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

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
                  () -> assertThat(caseView.getMetadataFields().get(0).getValue(), equalTo(CASE_TYPE_ID)),
                  () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                  () -> assertThat(caseView.getEvents(),
                                   hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_1)))),
                  () -> assertThat(caseView.getEvents(),
                                   hasItemInArray(hasProperty("summary", equalTo(EVENT_SUMMARY_2)))),
                  () -> assertThat(caseView.getState().getId(), is(STATE)),
                  () -> assertThat(caseView.getState().getTitleDisplay(), is(TITLE_DISPLAY))
        );
    }

    @Test
    @DisplayName("should retrieve only the authorised audit events")
    void shouldRetrieveOnlyAuthorisedAuditEvents() {
        Map<String, JsonNode> dataMap = buildData("dataTestField2");
        caseDetails.setData(dataMap);

        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

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
