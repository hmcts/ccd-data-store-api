package uk.gov.hmcts.ccd.v2.internal.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.DraftViewResource;

class UIDraftsControllerTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "1234123412341238";
    private static final String DRAFT_ID = "DRAFT128";
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().build();
    private static final DraftResponse DRAFT_RESPONSE = newDraftResponse().build();

    @Mock
    private CaseView caseView;
    @Mock
    private UpsertDraftOperation upsertDraftOperation;
    @Mock
    private GetCaseViewOperation getDraftViewOperation;
    @Mock
    private DraftGateway draftGateway;
    @InjectMocks
    private UIDraftsController draftsController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(caseView.getCaseId()).thenReturn(CASE_REFERENCE);
        when(upsertDraftOperation.executeSave(CASE_TYPE_ID, CASE_DATA_CONTENT)).thenReturn(DRAFT_RESPONSE);
        when(upsertDraftOperation.executeUpdate(CASE_TYPE_ID, DRAFT_ID, CASE_DATA_CONTENT)).thenReturn(DRAFT_RESPONSE);
        when(getDraftViewOperation.execute(DRAFT_ID)).thenReturn(caseView);
    }

    @Nested
    @DisplayName("POST /internal/case-types/{ctid}/drafts")
    class SaveDraft {

        @Test
        @DisplayName("should return 201 and draft response as body when draft saved")
        void shouldReturn201AndDraftResponseAsBodyWhenDraftSaved() {
            ResponseEntity<DraftViewResource> draftResponse = draftsController.saveDraft(CASE_TYPE_ID, CASE_DATA_CONTENT);

            assertAll(
                () -> assertThat(draftResponse.getStatusCode(), is(HttpStatus.CREATED)),
                () -> assertThat(draftResponse.getBody().getDraftResponse(), is(DRAFT_RESPONSE))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(upsertDraftOperation.executeSave(CASE_TYPE_ID, CASE_DATA_CONTENT)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class, () -> draftsController.saveDraft(CASE_TYPE_ID, CASE_DATA_CONTENT));
        }
    }

    @Nested
    @DisplayName("PUT /internal/case-types/{ctid}/drafts/{did}")
    class UpdateDraft {

        @Test
        @DisplayName("should return 200 when draft updated")
        void shouldReturn200WhenDraftUpdated() {
            ResponseEntity<DraftViewResource> draftResponse = draftsController.updateDraft(CASE_TYPE_ID, DRAFT_ID, CASE_DATA_CONTENT);

            assertAll(
                () -> assertThat(draftResponse.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(draftResponse.getBody().getDraftResponse(), is(DRAFT_RESPONSE))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(upsertDraftOperation.executeUpdate(CASE_TYPE_ID, DRAFT_ID, CASE_DATA_CONTENT)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class, () -> draftsController.updateDraft(CASE_TYPE_ID, DRAFT_ID, CASE_DATA_CONTENT));
        }
    }

    @Nested
    @DisplayName("GET /internal/drafts/{did}")
    class GetDraft {

        @Test
        @DisplayName("should return 200 when draft is retrieved")
        void shouldReturn200WhenDraftIsRetrieved() {
            ResponseEntity<CaseViewResource> draftResponse = draftsController.findDraft(DRAFT_ID);

            assertAll(
                () -> assertThat(draftResponse.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(draftResponse.getBody().getReference(), is(CASE_REFERENCE))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getDraftViewOperation.execute(DRAFT_ID)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class, () -> draftsController.findDraft(DRAFT_ID));
        }
    }

    @Nested
    @DisplayName("DELETE /internal/drafts/{did}")
    class DeleteDraft {

        @Test
        @DisplayName("should return 200 when draft is deleted")
        void shouldReturn200WhenDraftIsDeleted() {
            ResponseEntity<Void> responseEntity = draftsController.deleteDraft(DRAFT_ID);

            assertAll(
                () -> assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            doThrow(RuntimeException.class).when(draftGateway).delete(DRAFT_ID);

            assertThrows(Exception.class, () -> draftsController.deleteDraft(DRAFT_ID));
        }
    }
}
