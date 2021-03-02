package uk.gov.hmcts.ccd.data.draft;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CreateCaseDraftBuilder.newCreateCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.UpdateCaseDraftBuilder.newUpdateCaseDraft;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;

class CachedDraftGatewayTest {

    @Mock
    private uk.gov.hmcts.ccd.data.draft.DraftGateway defaultDraftGateway;
    @Mock
    private DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    private CreateCaseDraftRequest createCaseDraftRequest = newCreateCaseDraft().build();
    private UpdateCaseDraftRequest updateCaseDraftRequest = newUpdateCaseDraft().build();
    private Long draftId = 1L;
    private Long draftId2 = 2L;
    private String draftIdS = "1";
    private DraftResponse draftResponse = newDraftResponse().build();
    private List<DraftResponse> allDrafts = Lists.newArrayList(newDraftResponse().build());
    private CaseDetails caseDetails = newCaseDetails().build();

    private uk.gov.hmcts.ccd.data.draft.CachedDraftGateway cachedDraftGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(draftId).doReturn(draftId2).when(defaultDraftGateway).create(createCaseDraftRequest);
        doReturn(draftResponse).when(defaultDraftGateway).get(draftIdS);
        doReturn(allDrafts).when(defaultDraftGateway).getAll();
        doReturn(draftResponse).when(defaultDraftGateway).update(updateCaseDraftRequest, draftIdS);
        doReturn(caseDetails).when(draftResponseToCaseDetailsBuilder).build(draftResponse);

        cachedDraftGateway = new uk.gov.hmcts.ccd.data.draft.CachedDraftGateway(defaultDraftGateway,
                draftResponseToCaseDetailsBuilder);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should always create draft by calling decorated repository")
        void shouldAlwaysCallThrough() {

            Long result = cachedDraftGateway.create(createCaseDraftRequest);
            Long result2 = cachedDraftGateway.create(createCaseDraftRequest);

            assertAll(
                () -> assertThat(result, equalTo(draftId)),
                () -> assertThat(result2, equalTo(draftId2)),
                () -> verify(defaultDraftGateway, times(2)).create(createCaseDraftRequest),
                () -> verifyNoMoreInteractions(defaultDraftGateway)
            );
        }
    }

    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("should initially retrieve draft from decorated repository")
        void shouldRetrieveDraftFromDecorated() {

            DraftResponse result = cachedDraftGateway.get(draftIdS);

            assertAll(
                () -> assertThat(result, equalTo(draftResponse)),
                () -> verify(defaultDraftGateway).get(draftIdS)
            );
        }

        @Test
        @DisplayName("should cache drafts for subsequent calls")
        void shouldCacheDraftsForSubsequentCalls() {
            cachedDraftGateway.get(draftIdS);

            verify(defaultDraftGateway).get(draftIdS);

            doReturn(newDraftResponse().build()).when(defaultDraftGateway).get(draftIdS);

            DraftResponse result = cachedDraftGateway.get(draftIdS);

            assertAll(
                () -> assertThat(result, is(draftResponse)),
                () -> verifyNoMoreInteractions(defaultDraftGateway)
            );

        }
    }

    @Nested
    @DisplayName("getCaseDetails()")
    class GetCaseDetails {

        @Test
        @DisplayName("should initially retrieve draft from decorated repository")
        void shouldRetrieveDraftFromDecorated() {

            CaseDetails result = cachedDraftGateway.getCaseDetails(draftIdS);

            InOrder inOrder = inOrder(defaultDraftGateway, draftResponseToCaseDetailsBuilder);
            assertAll(
                () -> assertThat(result, equalTo(caseDetails)),
                () -> inOrder.verify(defaultDraftGateway).get(draftIdS),
                () -> inOrder.verify(draftResponseToCaseDetailsBuilder).build(draftResponse),
                () -> inOrder.verifyNoMoreInteractions()
            );
        }

        @Test
        @DisplayName("should cache drafts for subsequent calls")
        void shouldCacheDraftsForSubsequentCalls() {
            cachedDraftGateway.getCaseDetails(draftIdS);

            verify(defaultDraftGateway).get(draftIdS);
            verify(draftResponseToCaseDetailsBuilder).build(draftResponse);

            doReturn(newDraftResponse().build()).when(defaultDraftGateway).get(draftIdS);
            CaseDetails result = cachedDraftGateway.getCaseDetails(draftIdS);

            assertAll(
                () -> assertThat(result, is(caseDetails)),
                () -> verify(draftResponseToCaseDetailsBuilder, times(2)).build(draftResponse),
                () -> verifyNoMoreInteractions(defaultDraftGateway)
            );

        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("should always get all by call decorated repository")
        void shouldAlwaysCallThrough() {

            List<DraftResponse> result = cachedDraftGateway.getAll();
            List<DraftResponse> result2 = cachedDraftGateway.getAll();

            assertAll(
                () -> assertThat(result, equalTo(allDrafts)),
                () -> assertThat(result2, equalTo(allDrafts)),
                () -> verify(defaultDraftGateway, times(2)).getAll(),
                () -> verifyNoMoreInteractions(defaultDraftGateway)
            );
        }

    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should always update by call decorated repository")
        void shouldAlwaysCallThrough() {

            DraftResponse result = cachedDraftGateway.update(updateCaseDraftRequest, draftIdS);
            DraftResponse result2 = cachedDraftGateway.update(updateCaseDraftRequest, draftIdS);

            assertAll(
                () -> assertThat(result, equalTo(draftResponse)),
                () -> assertThat(result2, equalTo(draftResponse)),
                () -> verify(defaultDraftGateway, times(2)).update(updateCaseDraftRequest, draftIdS),
                () -> verifyNoMoreInteractions(defaultDraftGateway)
            );
        }

    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should always delete by call decorated repository")
        void shouldAlwaysCallThrough() {

            cachedDraftGateway.delete(draftIdS);
            cachedDraftGateway.delete(draftIdS);

            assertAll(
                () -> verify(defaultDraftGateway, times(2)).delete(draftIdS),
                () -> verifyNoMoreInteractions(defaultDraftGateway)
            );
        }

    }
}
