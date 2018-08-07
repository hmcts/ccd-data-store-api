package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.draft.CaseDraftBuilder.aCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftResponseBuilder.aDraftResponse;
import static uk.gov.hmcts.ccd.domain.model.std.CaseDataContentBuilder.aCaseDataContent;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.caseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTabCollectionBuilder.aCaseTabCollection;

class DefaultGetDraftViewOperationTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String DRAFT_ID_FORMAT = "DRAFT%s";
    private static final String DRAFT_ID = "1";
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
    private DraftGateway getDraftOperation;

    private GetCaseViewOperation getDraftViewOperation;

    private CaseTabCollection caseTabCollection;
    private CaseType caseType;
    private DraftResponse draftResponse;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        draftResponse = aDraftResponse()
            .withId(DRAFT_ID)
            .withDocument(aCaseDraft()
                              .withCaseTypeId(CASE_TYPE_ID)
                              .withEventTriggerId(EVENT_TRIGGER_ID)
                              .withCaseDataContent(aCaseDataContent()
                                                       .withData(caseData()
                                                                     .withPair("dataTestField1", JSON_NODE_FACTORY.textNode("dataTestField1"))
                                                                     .withPair("dataTestField2", JSON_NODE_FACTORY.textNode("dataTestField2"))
                                                                     .build())
                                                       .withEvent(anEvent()
                                                                      .withEventId(EVENT_TRIGGER_ID)
                                                                      .withDescription(EVENT_DESCRIPTION)
                                                                      .build())
                                                       .build())
                              .build())
            .build();

        doReturn(draftResponse).when(getDraftOperation).get(DRAFT_ID);

        caseTabCollection = aCaseTabCollection().withFieldIds("dataTestField1", "dataTestField2").build();
        doReturn(caseTabCollection).when(uiDefinitionRepository).getCaseTabCollection(CASE_TYPE_ID);

        caseType = new CaseType();
        Jurisdiction jurisdiction = new Jurisdiction();
        jurisdiction.setName(JURISDICTION_ID);
        caseType.setJurisdiction(jurisdiction);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

        getDraftViewOperation = new DefaultGetDraftViewOperation(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService, getDraftOperation);
    }

    @Test
    void shouldReturnDraftView() {
        CaseView caseView = getDraftViewOperation.execute(JURISDICTION_ID,
                                                          CASE_TYPE_ID,
                                                          DRAFT_ID);

        assertAll(() -> verify(getDraftOperation).get(DRAFT_ID),
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
                  () -> assertThat(caseView.getTriggers(), arrayWithSize(2)),
                  () -> assertThat(caseView.getTriggers()[0],
                                   allOf(hasProperty("id", equalTo(EVENT_TRIGGER_ID)),
                                         hasProperty("name", equalTo("Resume")),
                                         hasProperty("description", equalTo(EVENT_DESCRIPTION)),
                                         hasProperty("order", equalTo(1)))),
                  () -> assertThat(caseView.getTriggers()[1],
                                   allOf(hasProperty("id", is(nullValue())),
                                         hasProperty("name", equalTo("Delete")),
                                         hasProperty("description", equalTo("Delete draft")),
                                         hasProperty("order", equalTo(2)))),
                  () -> assertThat(caseView.getState(), is(nullValue())),
                  () -> assertThat(caseView.getEvents(), is(arrayWithSize(0)))
        );
    }
}
