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
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
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
    private static final String DATA_TEST_FIELD_1 = "dataTestField1";
    private static final String DATA_TEST_FIELD_2 = "dataTestField2";
    private static final String COMPLEX_FIELD_TYPE = "Complex";
    private static final String FAMILY_ADDRESS_COUNTRY = "FamilyAddress.Country";
    private static final String FAMILY_ADDRESS_POST_CODE = "FamilyAddress.PostCode";
    private static final String DATA_TEST_FIELD_1_FAMILY_ADDRESS_COUNTRY =
        DATA_TEST_FIELD_1 + "." + FAMILY_ADDRESS_COUNTRY;
    private static final String DATA_TEST_FIELD_1_FAMILY_ADDRESS_POST_CODE =
        DATA_TEST_FIELD_1 + "." + FAMILY_ADDRESS_POST_CODE;
    private static final String DATA_TEST_FIELD_1_SHOW_CONDITION = DATA_TEST_FIELD_1 + "-fieldShowCondition";
    private static final String DATA_TEST_FIELD_2_SHOW_CONDITION = DATA_TEST_FIELD_2 + "-fieldShowCondition";
    private static final int NO_FIELD_COUNT = 0;
    private static final int NO_ACTIONABLE_EVENT_COUNT = 0;
    private static final int SINGLE_TAB_COUNT = 1;
    private static final int SINGLE_FIELD_COUNT = 1;
    private static final int SINGLE_ACTIONABLE_EVENT_COUNT = 1;
    private static final int TWO_FIELD_COUNT = 2;
    private static final int TWO_EVENT_COUNT = 2;
    private static final int TWO_METADATA_FIELD_COUNT = 2;
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
        MockitoAnnotations.openMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setReference(Long.valueOf(CASE_REFERENCE));
        caseDetails.setState(STATE);
        Map<String, JsonNode> dataMap = buildData(DATA_TEST_FIELD_1, DATA_TEST_FIELD_2);
        caseDetails.setData(dataMap);
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);

        event1 = new AuditEvent();
        event1.setSummary(EVENT_SUMMARY_1);
        event2 = new AuditEvent();
        event2.setSummary(EVENT_SUMMARY_2);
        auditEvents = asList(event1, event2);
        doReturn(auditEvents).when(getEventsOperation).getEvents(caseDetails);

        doReturn(eventsNode).when(objectMapperService).convertJsonNodeToMap(any());

        doReturn(Boolean.TRUE).when(uidService).validateUID(CASE_REFERENCE);

        caseTypeTabsDefinition = newCaseTabCollection().withFieldIds(DATA_TEST_FIELD_1, DATA_TEST_FIELD_2).build();
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
                () -> assertThat(caseView.getTabs(), arrayWithSize(SINGLE_TAB_COUNT)),
                () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(SINGLE_FIELD_COUNT)),
                () -> assertThat(caseView.getTabs()[0].getFields()[0], hasProperty("id",
                    equalTo(CASE_HISTORY_VIEWER))),
                () -> assertThat(caseView.getTabs()[0].getFields()[0], hasProperty("value",
                    equalTo(eventsNode))),
                () -> assertThat(caseView.getEvents(), arrayWithSize(TWO_EVENT_COUNT)),
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
                () -> assertThat(caseView.getTabs(), arrayWithSize(SINGLE_TAB_COUNT)),
                () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(NO_FIELD_COUNT)),
                () -> assertThat(caseView.getEvents(), arrayWithSize(TWO_EVENT_COUNT)),
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
            caseEventDefinition.setEndButtonLabel(DATA_TEST_FIELD_1 + "=\"" + DATA_TEST_FIELD_1 + "\"");
            caseTypeDefinition.setEvents(Lists.newArrayList(caseEventDefinition));
            doReturn(true).when(eventTriggerService).isPreStateValid(anyString(), any());
            doReturn(true).when(caseEventEnablingService).isEventEnabled(any(), any(CaseDetails.class), anyList());

            CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);
            assertNotNull(caseView);
            assertEquals(SINGLE_ACTIONABLE_EVENT_COUNT, caseView.getActionableEvents().length);
        }

        @Test
        @DisplayName("should filter event when enabling condition is not valid")
        void shouldFilterEventWhenEnablingConditionIsNotValid() {
            CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setEndButtonLabel(DATA_TEST_FIELD_1 + "=\"" + DATA_TEST_FIELD_1 + "\" AND "
                + DATA_TEST_FIELD_2 + "=\"Test\"");
            caseTypeDefinition.setEvents(Lists.newArrayList(caseEventDefinition));
            doReturn(true).when(eventTriggerService).isPreStateValid(anyString(), any());
            doReturn(false).when(caseEventEnablingService).isEventEnabled(any(), any(CaseDetails.class), anyList());

            CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);
            assertNotNull(caseView);
            assertEquals(NO_ACTIONABLE_EVENT_COUNT, caseView.getActionableEvents().length);
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
            () -> assertThat(caseView.getTabs(), arrayWithSize(SINGLE_TAB_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(TWO_FIELD_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields(),
                             hasItemInArray(allOf(hasProperty("id", equalTo(DATA_TEST_FIELD_1)),
                                                  hasProperty("showCondition",
                                                              equalTo(DATA_TEST_FIELD_1_SHOW_CONDITION))))),
            () -> assertThat(caseView.getTabs()[0].getFields(),
                             hasItemInArray(allOf(hasProperty("id", equalTo(DATA_TEST_FIELD_2)),
                                                  hasProperty("showCondition",
                                                              equalTo(DATA_TEST_FIELD_2_SHOW_CONDITION))))),
            () -> assertThat(caseView.getMetadataFields().get(0).getValue(), equalTo(CASE_TYPE_ID)),
            () -> assertThat(caseView.getEvents(), arrayWithSize(TWO_EVENT_COUNT)),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                equalTo(EVENT_SUMMARY_1)))),
            () -> assertThat(caseView.getEvents(), hasItemInArray(hasProperty("summary",
                equalTo(EVENT_SUMMARY_2)))),
            () -> assertThat(caseView.getState().getId(), is(STATE)),
            () -> assertThat(caseView.getState().getTitleDisplay(), is(TITLE_DISPLAY))
        );
    }

    @Test
    @DisplayName("should preserve fully qualified CaseFieldSubfieldCode in case view tab fields")
    void shouldPreserveFullyQualifiedCaseFieldSubfieldCodeInCaseViewTabFields() {
        caseTypeTabsDefinition = newCaseTabCollection()
            .withTab(newCaseTab()
                .withTabField(caseTabField(DATA_TEST_FIELD_1, COMPLEX_FIELD_TYPE,
                    DATA_TEST_FIELD_1_FAMILY_ADDRESS_COUNTRY))
                .build())
            .build();
        doReturn(caseTypeTabsDefinition).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

        assertAll(
            () -> assertThat(caseView.getTabs(), arrayWithSize(SINGLE_TAB_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(SINGLE_FIELD_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields()[0],
                allOf(hasProperty("id", equalTo(DATA_TEST_FIELD_1)),
                    hasProperty("caseFieldSubfieldCode", equalTo(DATA_TEST_FIELD_1_FAMILY_ADDRESS_COUNTRY))))
        );
    }

    @Test
    @DisplayName("should return fully qualified CaseFieldSubfieldCode when CaseTypeTab uses relative subfield code")
    void shouldReturnFullyQualifiedCaseFieldSubfieldCodeFromRelativeSubfieldCode() {
        caseTypeTabsDefinition = newCaseTabCollection()
            .withTab(newCaseTab()
                .withTabField(caseTabField(DATA_TEST_FIELD_1, COMPLEX_FIELD_TYPE, FAMILY_ADDRESS_COUNTRY))
                .build())
            .build();
        doReturn(caseTypeTabsDefinition).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

        assertAll(
            () -> assertThat(caseView.getTabs(), arrayWithSize(SINGLE_TAB_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(SINGLE_FIELD_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields()[0],
                allOf(hasProperty("id", equalTo(DATA_TEST_FIELD_1)),
                    hasProperty("caseFieldSubfieldCode", equalTo(DATA_TEST_FIELD_1_FAMILY_ADDRESS_COUNTRY))))
        );
    }

    @Test
    @DisplayName("should return fully qualified CaseFieldSubfieldCode for multiple relative subfield codes")
    void shouldReturnFullyQualifiedCaseFieldSubfieldCodeForMultipleRelativeSubfieldCodes() {
        CaseFieldDefinition dataTestField1 = caseField(DATA_TEST_FIELD_1, COMPLEX_FIELD_TYPE);

        caseTypeTabsDefinition = newCaseTabCollection()
            .withTab(newCaseTab()
                .withTabField(caseTabField(dataTestField1, FAMILY_ADDRESS_COUNTRY))
                .withTabField(caseTabField(dataTestField1, FAMILY_ADDRESS_POST_CODE))
                .build())
            .build();
        doReturn(caseTypeTabsDefinition).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

        assertAll(
            () -> assertThat(caseView.getTabs(), arrayWithSize(SINGLE_TAB_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(TWO_FIELD_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields(),
                hasItemInArray(hasProperty("caseFieldSubfieldCode",
                    equalTo(DATA_TEST_FIELD_1_FAMILY_ADDRESS_COUNTRY)))),
            () -> assertThat(caseView.getTabs()[0].getFields(),
                hasItemInArray(hasProperty("caseFieldSubfieldCode",
                    equalTo(DATA_TEST_FIELD_1_FAMILY_ADDRESS_POST_CODE))))
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
            () -> assertThat(caseView.getMetadataFields().size(), equalTo(TWO_METADATA_FIELD_COUNT)),
            () -> assertThat(caseView.getMetadataFields().get(0).getId(), equalTo("[CASE_TYPE]")),
            () -> assertThat(caseView.getMetadataFields().get(0).getValue(), equalTo(CASE_TYPE_ID)),
            () -> assertThat(caseView.getMetadataFields().get(1).getId(), equalTo(GET_CASE_METADATA_FIELD_ID)),
            () -> assertThat(caseView.getMetadataFields().get(1).getValue(), equalTo(GET_CASE_METADATA_FIELD_VALUE))
        );
    }

    @Test
    @DisplayName("should retrieve only the authorised audit events")
    void shouldRetrieveOnlyAuthorisedAuditEvents() {
        Map<String, JsonNode> dataMap = buildData(DATA_TEST_FIELD_2);
        caseDetails.setData(dataMap);

        final CaseView caseView = defaultGetCaseViewOperation.execute(CASE_REFERENCE);

        assertAll(
            () -> verify(getEventsOperation).getEvents(caseDetails),
            () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(SINGLE_FIELD_COUNT)),
            () -> assertThat(caseView.getTabs()[0].getFields(), hasItemInArray(hasProperty("id",
                equalTo(DATA_TEST_FIELD_2)))),
            () -> assertThat(caseView.getEvents(), arrayWithSize(TWO_EVENT_COUNT)),
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

    private CaseTypeTabField caseTabField(String fieldId, String fieldType, String caseFieldSubfieldCode) {
        return caseTabField(caseField(fieldId, fieldType), caseFieldSubfieldCode);
    }

    private CaseTypeTabField caseTabField(CaseFieldDefinition caseField, String caseFieldSubfieldCode) {
        return newCaseTabField()
            .withCaseField(caseField)
            .withCaseFieldSubfieldCode(caseFieldSubfieldCode)
            .build();
    }

    private CaseFieldDefinition caseField(String fieldId, String fieldType) {
        return newCaseField()
            .withId(fieldId)
            .withFieldType(aFieldType()
                .withType(fieldType)
                .build())
            .build();
    }

    private Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = new HashMap<>();
        asList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }

}
