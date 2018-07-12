package uk.gov.hmcts.ccd.domain.service.getdraft;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import java.util.Optional;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.draft.CaseDraftBuilder.aCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftResponseBuilder.aDraftResponse;
import static uk.gov.hmcts.ccd.domain.model.std.CaseDataContentBuilder.aCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.aCaseData;

class DefaultGetDraftOperationTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String DID = "5";
    private static final String JID = "Probate";
    private static final String CTID = "Grant";

    @Mock
    private DraftGateway draftGateway;

    private GetDraftOperation getDraftOperation;

    private DraftResponse draft = aDraftResponse()
        .withId(DID)
        .withDocument(aCaseDraft()
                          .withJurisdictionId(JID)
                          .withCaseTypeId(CTID)
                          .withCaseDataContent(aCaseDataContent()
                                                   .withData(aCaseData()
                                                                 .withPair("key", JSON_NODE_FACTORY.textNode("value"))
                                                                 .build())
                                                   .build())
                          .build())
        .build();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        getDraftOperation = new DefaultGetDraftOperation(draftGateway);
    }

    @Test
    void shouldSuccessfullyGetDraft() {
        doReturn(draft).when(draftGateway).get(DID);

        Optional<CaseDetails> caseDetailsOptional = getDraftOperation.execute(DID);

        assertAll(
            () -> verify(draftGateway).get(DID),
            () -> assertThat(caseDetailsOptional.isPresent(), is(true)),
            () -> assertThat(caseDetailsOptional.get(), hasProperty("id", is(Long.valueOf(draft.getId())))),
            () -> assertThat(caseDetailsOptional.get(), hasProperty("jurisdiction", is(draft.getDocument().getJurisdictionId()))),
            () -> assertThat(caseDetailsOptional.get(), hasProperty("caseTypeId", is(draft.getDocument().getCaseTypeId()))),
            () -> assertThat(caseDetailsOptional.get(), hasProperty("data", is(draft.getDocument().getCaseDataContent().getData())))
        );
    }
}
