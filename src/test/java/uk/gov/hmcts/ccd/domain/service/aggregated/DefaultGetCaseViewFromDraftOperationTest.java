package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.CASE_HISTORY_VIEWER;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCaseViewFromDraftOperation.DELETE;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.newCaseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTabCollectionBuilder.newCaseTabCollection;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeTabBuilder.newCaseTab;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeTabFieldBuilder.newCaseTabField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class DefaultGetCaseViewFromDraftOperationTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String DRAFT_ID_FORMAT = "DRAFT%s";
    private static final String DRAFT_ID = "1";
    private static final String DRAFT_ID_FOR_UI = "DRAFT1";
    private static final String EVENT_TRIGGER_ID = "createCase";
    private static final String EVENT_DESCRIPTION = "Create case";

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private UIDService uidService;

    @Mock
    private DraftGateway draftGateway;

    @Mock
    private DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    @Mock
    private ObjectMapperService objectMapperService;

    @Mock
    private CompoundFieldOrderService compoundFieldOrderService;

    @Mock
    private FieldProcessorService fieldProcessorService;

    private uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation getDraftViewOperation;

    private CaseTypeDefinition caseTypeDefinition;
    private CaseTypeTabsDefinition caseTypeTabsDefinition;
    private DraftResponse draftResponse;
    private CaseDetails caseDetails;
    private Map<String, JsonNode> data;
    private JsonNode eventsNode;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime then = LocalDateTime.now();
        data = newCaseData()
            .withPair("dataTestField1", JSON_NODE_FACTORY.textNode("dataTestField1"))
            .withPair("dataTestField2", JSON_NODE_FACTORY.textNode("dataTestField2"))
            .build();
        draftResponse = newDraftResponse()
            .withId(DRAFT_ID)
            .withDocument(newCaseDraft()
                              .withCaseTypeId(CASE_TYPE_ID)
                              .withEventId(EVENT_TRIGGER_ID)
                              .withCaseDataContent(newCaseDataContent()
                                                       .withData(data)
                                                       .withEvent(anEvent()
                                                                      .withEventId(EVENT_TRIGGER_ID)
                                                                      .withDescription(EVENT_DESCRIPTION)
                                                                      .build())
                                                       .build())
                              .build())
            .withCreated(then)
            .withUpdated(now)
            .build();


        doReturn(draftResponse).when(draftGateway).get(DRAFT_ID);
        caseDetails = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID)
            .withJurisdiction(JURISDICTION_ID)
            .withId(DRAFT_ID_FOR_UI)
            .withData(data)
            .build();
        doReturn(caseDetails).when(draftResponseToCaseDetailsBuilder).build(draftResponse);

        caseTypeTabsDefinition = newCaseTabCollection().withFieldIds("dataTestField1", "dataTestField2")
                                                  .build();
        doReturn(caseTypeTabsDefinition).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        caseTypeDefinition = new CaseTypeDefinition();
        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setName(JURISDICTION_ID);
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        doReturn(caseTypeDefinition).when(caseTypeService).getCaseType(CASE_TYPE_ID);

        doReturn(eventsNode).when(objectMapperService).convertJsonNodeToMap(anyObject());

        doAnswer(invocation -> invocation.getArgument(0)).when(fieldProcessorService).processCaseViewField(any());

        getDraftViewOperation = new uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCaseViewFromDraftOperation(
                getCaseOperation,
                uiDefinitionRepository,
                caseTypeService,
                uidService,
                draftGateway,
                draftResponseToCaseDetailsBuilder,
                objectMapperService,
                compoundFieldOrderService,
                fieldProcessorService);
    }

    @Test
    void shouldReturnDraftView() {
        CaseView caseView = getDraftViewOperation.execute(DRAFT_ID);

        assertAll(() -> verify(draftGateway).get(DRAFT_ID),
            () -> assertThat(caseView.getCaseId(), is(String.format(DRAFT_ID_FORMAT, DRAFT_ID))),
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
            () -> assertThat(caseView.getActionableEvents(), arrayWithSize(2)),
            () -> assertThat(caseView.getActionableEvents()[0],
                allOf(hasProperty("id", equalTo(EVENT_TRIGGER_ID)),
                    hasProperty("name", equalTo("Resume")),
                    hasProperty("description", equalTo(EVENT_DESCRIPTION)),
                    hasProperty("order", equalTo(1)))),
            () -> assertThat(caseView.getActionableEvents()[1],
                allOf(hasProperty("id", is(DELETE)),
                    hasProperty("name", equalTo("Delete")),
                    hasProperty("description", equalTo("Delete draft")),
                    hasProperty("order", equalTo(2)))),
            () -> assertThat(caseView.getEvents(), is(arrayWithSize(2))),
            () -> assertThat(caseView.getEvents()[0],
                allOf(hasProperty("eventId", equalTo("Draft updated")),
                    hasProperty("eventName", equalTo("Draft updated")),
                    hasProperty("stateId", equalTo("Draft")),
                    hasProperty("stateName", equalTo("Draft")))),
            () -> assertThat(caseView.getEvents()[1],
                allOf(hasProperty("eventId", equalTo("Draft created")),
                    hasProperty("eventName", equalTo("Draft created")),
                    hasProperty("stateId", equalTo("Draft")),
                    hasProperty("stateName", equalTo("Draft"))))
        );
    }

    @Nested
    @DisplayName("field of CaseHistoryViewer field type")
    class CaseHistoryViewerFieldType {
        @Test
        @DisplayName("should hydrate case history viewer if CaseHistoryViewer field type present in tabs")
        void shouldHydrateCaseHistoryViewerIfFieldPresentInTabs() {
            caseTypeTabsDefinition = newCaseTabCollection().withTab(newCaseTab()
                .withTabField(newCaseTabField()
                    .withCaseField(newCaseField()
                        .withId(CASE_HISTORY_VIEWER)
                        .withFieldType(aFieldType()
                            .withType(
                                CASE_HISTORY_VIEWER)
                            .build())

                        .build())
                    .withDisplayContextParameter(null)
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

            final CaseView caseView = getDraftViewOperation.execute(DRAFT_ID);

            assertAll(() -> assertThat(caseView.getTabs(), arrayWithSize(1)),
                () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(1)),
                () -> assertThat(caseView.getTabs()[0].getFields()[0],
                        hasProperty("id", equalTo(CASE_HISTORY_VIEWER))),
                () -> assertThat(caseView.getTabs()[0].getFields()[0], hasProperty("value", equalTo(eventsNode))),
                () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                () -> assertThat(caseView.getEvents()[0],
                    allOf(hasProperty("eventId", equalTo("Draft updated")),
                        hasProperty("eventName", equalTo("Draft updated")),
                        hasProperty("stateId", equalTo("Draft")),
                        hasProperty("stateName", equalTo("Draft")))),
                () -> assertThat(caseView.getEvents()[1],
                    allOf(hasProperty("eventId", equalTo("Draft created")),
                        hasProperty("eventName", equalTo("Draft created")),
                        hasProperty("stateId", equalTo("Draft")),
                        hasProperty("stateName", equalTo("Draft"))))
            );
        }

        @Test
        @DisplayName("should not hydrate case history viewer if CaseHistoryViewer field type is not present in tabs")
        void shouldNotHydrateCaseHistoryViewerIfFieldIsNotPresentInTabs() {
            caseTypeTabsDefinition = newCaseTabCollection().withTab(newCaseTab()
                .withTabField(newCaseTabField()
                    .withCaseField(newCaseField()
                        .withId("NotACaseHistoryViewer")
                        .withFieldType(aFieldType()
                            .withType(
                                "NotACaseHistoryViewer")
                            .build())
                        .withDisplayContextParameter(null)
                        .build())
                    .withDisplayContextParameter(null)
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

            final CaseView caseView = getDraftViewOperation.execute(DRAFT_ID);

            assertAll(() -> assertThat(caseView.getTabs(), arrayWithSize(1)),
                () -> assertThat(caseView.getTabs()[0].getFields(), arrayWithSize(0)),
                () -> assertThat(caseView.getEvents(), arrayWithSize(2)),
                () -> assertThat(caseView.getEvents()[0],
                    allOf(hasProperty("eventId", equalTo("Draft updated")),
                        hasProperty("eventName", equalTo("Draft updated")),
                        hasProperty("stateId", equalTo("Draft")),
                        hasProperty("stateName", equalTo("Draft")))),
                () -> assertThat(caseView.getEvents()[1],
                    allOf(hasProperty("eventId", equalTo("Draft created")),
                        hasProperty("eventName", equalTo("Draft created")),
                        hasProperty("stateId", equalTo("Draft")),
                        hasProperty("stateName", equalTo("Draft"))))
            );
        }
    }

}
