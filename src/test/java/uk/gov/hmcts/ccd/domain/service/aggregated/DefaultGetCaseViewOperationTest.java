package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
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
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.callbacks.GetCaseCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseEventEnablingService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseCallback;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.CASE_HISTORY_VIEWER;
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
    public static final String GET_CASE_METADATA_FIELD_ID = "anotherFieldId";
    public static final String GET_CASE_METADATA_FIELD_VALUE = "getCaseMetadataFieldValue";

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

    @Mock
    private CaseEventEnablingService caseEventEnablingService;

    @Mock
    private GetCaseCallback getCaseCallback;

    @Spy
    @InjectMocks
    private DefaultGetCaseViewOperation defaultGetCaseViewOperation;

    private CaseDetails caseDetails;
    private List<AuditEvent> auditEvents;
    private AuditEvent event1;
    private AuditEvent event2;
    private CaseTypeTabsDefinition caseTypeTabsDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseStateDefinition caseStateDefinition;
    private JsonNode eventsNode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setReference(Long.valueOf(CASE_REFERENCE));
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

        caseTypeTabsDefinition = newCaseTabCollection().withFieldIds("dataTestField1", "dataTestField2").build();
        doReturn(caseTypeTabsDefinition).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        caseTypeDefinition = new CaseTypeDefinition();
        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setName(JURISDICTION_ID);
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(MetaData.CaseField.CASE_TYPE.getReference());
        caseFieldDefinition.setMetadata(true);
        caseFieldDefinition.setFieldTypeDefinition(new FieldTypeDefinition());
        caseTypeDefinition.setCaseFieldDefinitions(singletonList(caseFieldDefinition));

        doReturn(caseTypeDefinition).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

        caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId(STATE);
        caseStateDefinition.setTitleDisplay(TITLE_DISPLAY);
        doReturn(caseStateDefinition).when(caseTypeService).findState(caseTypeDefinition, STATE);

        doAnswer(invocation ->
            invocation.getArgument(0)).when(fieldProcessorService).processCaseViewField(any());
    }

    @Nested
    @DisplayName("field of CaseHistoryViewer field type")
    class CaseHistoryViewerFieldType {
        @Test
        @DisplayName("should hydrate case history viewer if CaseHistoryViewer field type present in tabs")
        void shouldHydrateCaseHistoryViewerIfFieldPresentInTabs() {
            caseTypeTabsDefinition =
                newCaseTabCollection()
                    .withTab(newCaseTab()
                        .withTabField(newCaseTabField()
                            .withCaseField(newCaseField()
                                .withId(CASE_HISTORY_VIEWER)
                                .withFieldType(aFieldType()
                                    .withType(CASE_HISTORY_VIEWER)
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build();
            doReturn(caseTypeTabsDefinition).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);
            caseTypeDefinition.setCaseFieldDefinitions(singletonList(newCaseField()
                .withId(CASE_HISTORY_VIEWER)
                .withFieldType(aFieldType()
                    .withType(CASE_HISTORY_VIEWER)
                    .build())
                .build()));
            doReturn(caseTypeDefinition).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID,
                JURISDICTION_ID);

            final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertThat(caseView.getTabs(), arrayWithSize(1)),
                () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(1)),
                () -> assertThat(caseView.getTabs()[0].getFields()[0], hasProperty("id",
                    equalTo(CASE_HISTORY_VIEWER))),
                () -> assertThat(caseView.getTabs()[0].getFields()[0], hasProperty("value",
                    equalTo(eventsNode))),
                () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                    equalTo(EVENT_SUMMARY_1)))),
                () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                    equalTo(EVENT_SUMMARY_2))))
            );
        }

        @Test
        @DisplayName("should not hydrate case history viewer if CaseHistoryViewer field type is not present in tabs")
        void shouldNotHydrateCaseHistoryViewerIfFieldIsNotPresentInTabs() {
            caseTypeTabsDefinition =
                newCaseTabCollection()
                    .withTab(newCaseTab()
                        .withTabField(newCaseTabField()
                            .withCaseField(newCaseField()
                                .withId("NotACaseHistoryViewer")
                                    .withFieldType(aFieldType()
                                        .withType("NotACaseHistoryViewer")
                                        .build())
                                    .build())
                            .build())
                         .build())
                    .build();
            doReturn(caseTypeTabsDefinition).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);
            caseTypeDefinition.setCaseFieldDefinitions(singletonList(newCaseField()
                                                     .withId(CASE_HISTORY_VIEWER)
                                                     .withFieldType(aFieldType()
                                                                        .withType(CASE_HISTORY_VIEWER)
                                                                        .build())
                                                     .build()));
            doReturn(caseTypeDefinition).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID,
                JURISDICTION_ID);

            final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertThat(caseView.getTabs(), arrayWithSize(1)),
                () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(0)),
                () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                    equalTo(EVENT_SUMMARY_1)))),
                () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                    equalTo(EVENT_SUMMARY_2))))
            );
        }
    }

    @Nested
    @DisplayName("Event Enabling condition test")
    class CaseEventEnablingCondition {

        @Test
        @DisplayName("should not filter event when enabling condition is valid")
        void shouldNotFilterEventWhenEnablingConditionIsValid() {
            CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setEndButtonLabel("dataTestField1=\"dataTestField1\"");
            caseTypeDefinition.setEvents(Lists.newArrayList(caseEventDefinition));
            doReturn(true).when(eventTriggerService).isPreStateValid(anyString(), any());
            doReturn(true).when(caseEventEnablingService).isEventEnabled(any(), any(CaseDetails.class), anyList());

            CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);
            assertNotNull(caseView);
            assertEquals(1, caseView.getActionableEvents().length);
        }

        @Test
        @DisplayName("should filter event when enabling condition is not valid")
        void shouldFilterEventWhenEnablingConditionIsNotValid() {
            CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setEndButtonLabel("dataTestField1=\"dataTestField1\" AND dataTestField2=\"Test\"");
            caseTypeDefinition.setEvents(Lists.newArrayList(caseEventDefinition));
            doReturn(true).when(eventTriggerService).isPreStateValid(anyString(), any());
            doReturn(false).when(caseEventEnablingService).isEventEnabled(any(), any(CaseDetails.class), anyList());

            CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);
            assertNotNull(caseView);
            assertEquals(0, caseView.getActionableEvents().length);
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

        assertAll(
            () -> verify(getEventsOperation).getEvents(caseDetails),
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
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                equalTo(EVENT_SUMMARY_1)))),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                equalTo(EVENT_SUMMARY_2)))),
            () -> assertThat(caseView.getState().getId(), is(STATE)),
            () -> assertThat(caseView.getState().getTitleDisplay(), is(TITLE_DISPLAY))
        );
    }

    @Test
    @DisplayName("should add metadata fields from the get case callback")
    void shouldAddMetadataFieldsFromTheGetCaseCallback() {
        caseTypeDefinition.setCallbackGetCaseUrl("/callback/getCase");
        GetCaseCallbackResponse callbackResponse = new GetCaseCallbackResponse();
        callbackResponse.setMetadataFields(singletonList(caseViewField()));
        doReturn(callbackResponse)
            .when(getCaseCallback).invoke(any(CaseTypeDefinition.class), any(CaseDetails.class), anyList());

        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

        assertAll(
            () -> assertThat(caseView.getMetadataFields().size(), equalTo(2)),
            () -> assertThat(caseView.getMetadataFields().get(0).getId(), equalTo("[CASE_TYPE]")),
            () -> assertThat(caseView.getMetadataFields().get(0).getValue(), equalTo(CASE_TYPE_ID)),
            () -> assertThat(caseView.getMetadataFields().get(1).getId(), equalTo(GET_CASE_METADATA_FIELD_ID)),
            () -> assertThat(caseView.getMetadataFields().get(1).getValue(), equalTo(GET_CASE_METADATA_FIELD_VALUE))
        );
    }

    @Test
    @DisplayName("should retrieve only the authorised audit events")
    void shouldRetrieveOnlyAuthorisedAuditEvents() {
        Map<String, JsonNode> dataMap = buildData("dataTestField2");
        caseDetails.setData(dataMap);

        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

        assertAll(
            () -> verify(getEventsOperation).getEvents(caseDetails),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(1)),
            () -> assertThat(caseView.getTabs()[0].getFields(), hasItemInArray(hasProperty("id",
                equalTo("dataTestField2")))),
            () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                equalTo(EVENT_SUMMARY_1)))),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                equalTo(EVENT_SUMMARY_2))))
        );
    }

    private CaseViewField caseViewField() {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(GET_CASE_METADATA_FIELD_ID);
        caseViewField.setValue(GET_CASE_METADATA_FIELD_VALUE);
        return caseViewField;
    }

    private Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = new HashMap<>();
        asList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }

}
