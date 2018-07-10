package uk.gov.hmcts.ccd.domain.service.getdraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftBuilder.aDraft;

class DefaultGetDraftOperationTest {

    private static final String DID = "5";

    @Mock
    private DraftGateway draftGateway;

    private GetDraftOperation getDraftOperation;

    private Draft draft = aDraft().build();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        getDraftOperation = new DefaultGetDraftOperation(draftGateway);
    }

    @Test
    void shouldSuccessfullyGetDraft() {
        doReturn(draft).when(draftGateway).get(DID);

        DraftResponse result = getDraftOperation.execute(DID);

        assertAll(
            () ->  verify(draftGateway).get(DID),
            () ->  assertThat(result, is(sameInstance(draft)))
        );
    }
}
