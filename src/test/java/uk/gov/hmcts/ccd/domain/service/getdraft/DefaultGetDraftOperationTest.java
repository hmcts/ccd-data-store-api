package uk.gov.hmcts.ccd.domain.service.getdraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftResponseBuilder.aDraftResponse;

class DefaultGetDraftOperationTest {

    private static final String DID = "5";

    @Mock
    private DraftGateway draftGateway;

    private GetDraftOperation getDraftOperation;

    private DraftResponse draft = aDraftResponse().build();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        getDraftOperation = new DefaultGetDraftOperation(draftGateway);
    }

    @Test
    void shouldReturnEmptyDraftIfNotPresent() {
        doReturn(null).when(draftGateway).get(DID);

        Optional<DraftResponse> result = getDraftOperation.execute(DID);

        assertAll(
            () ->  verify(draftGateway).get(DID),
            () ->  assertThat(result.isPresent(), is(false))
        );
    }

    @Test
    void shouldSuccessfullyGetDraftIfPresent() {
        doReturn(draft).when(draftGateway).get(DID);

        Optional<DraftResponse> result = getDraftOperation.execute(DID);

        assertAll(
            () ->  verify(draftGateway).get(DID),
            () ->  assertThat(result.isPresent(), is(true)),
            () ->  assertThat(result.get(), is(sameInstance(draft)))
        );
    }
}
