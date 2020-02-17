package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@DisplayName("CaseResource")
class CaseResourceTest {
    private static final Long REFERENCE = 1234123412341238L;
    private static final Boolean IGNORE_WARNING = true;
    private static final LocalDateTime CREATED_ON = LocalDateTime.now();
    private static final LocalDateTime LAST_MODIFIED_ON = LocalDateTime.now();
    private static final String DRAFT_ID = "123";
    private static final String JURISDICTION = "Test";
    private static final String CASE_TYPE = "Demo";
    private static final String STATE = "Started";
    private static final String CALLBACK_COMPLETED = "CALLBACK_COMPLETED";
    private static final SecurityClassification SECURITY_CLASSIFICATION = SecurityClassification.PUBLIC;
    private static final Map<String, JsonNode> DATA = Collections.singletonMap("FieldID", new TextNode("Value"));
    private static final Map<String, JsonNode> DATA_CLASSIFICATION = Collections.singletonMap("FieldID", new TextNode("PUBLIC"));
    private static final AfterSubmitCallbackResponse CALLBACK_BODY = new AfterSubmitCallbackResponse();
    private static final ResponseEntity<AfterSubmitCallbackResponse> AFTER_SUBMIT_CALLBACK_RESPONSE = ResponseEntity.ok(CALLBACK_BODY);
    private static final String DELETE_DRAFT_COMPLETED = "DELETE_DRAFT_COMPLETED";
    private static final ResponseEntity<Void> DELETE_DRAFT_RESPONSE = ResponseEntity.ok().build();

    private CaseDetails caseDetails;
    private CaseDataContent caseDataContent;

    @BeforeEach
    void setUp() {
        caseDetails = aCaseDetails();
        caseDataContent = newCaseDataContent().build();
    }

    @Nested
    @DisplayName("Get case")
    class GetCase {

        private final String LINK_SELF = String.format("/cases/%s", REFERENCE);

        @Test
        @DisplayName("should copy case details")
        void shouldCopyCaseDetails() {
            final CaseResource caseResource = new CaseResource(caseDetails);

            assertAll(
                () -> assertThat(caseResource.getReference(), equalTo(REFERENCE.toString())),
                () -> assertThat(caseResource.getCreatedOn(), equalTo(CREATED_ON)),
                () -> assertThat(caseResource.getLastModifiedOn(), equalTo(LAST_MODIFIED_ON)),
                () -> assertThat(caseResource.getJurisdiction(), equalTo(JURISDICTION)),
                () -> assertThat(caseResource.getCaseType(), equalTo(CASE_TYPE)),
                () -> assertThat(caseResource.getState(), equalTo(STATE)),
                () -> assertThat(caseResource.getSecurityClassification(), equalTo(SECURITY_CLASSIFICATION)),
                () -> assertThat(caseResource.getData(), equalTo(DATA)),
                () -> assertThat(caseResource.getDataClassification(), equalTo(DATA_CLASSIFICATION)),
                () -> assertThat(caseResource.getAfterSubmitCallbackResponse(), equalTo(CALLBACK_BODY)),
                () -> assertThat(caseResource.getCallbackResponseStatusCode(), equalTo(AFTER_SUBMIT_CALLBACK_RESPONSE.getStatusCodeValue())),
                () -> assertThat(caseResource.getCallbackResponseStatus(), equalTo(CALLBACK_COMPLETED)),
                () -> assertThat(caseResource.getDeleteDraftResponseStatusCode(), equalTo(DELETE_DRAFT_RESPONSE.getStatusCodeValue())),
                () -> assertThat(caseResource.getDeleteDraftResponseStatus(), equalTo(DELETE_DRAFT_COMPLETED))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final CaseResource caseResource = new CaseResource(caseDetails);

            Optional<Link> self = caseResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(LINK_SELF));
        }
    }

    @Nested
    @DisplayName("Create event")
    class CreateEvent {

        private final String LINK_SELF = String.format("/cases/%s/events", REFERENCE);

        @Test
        @DisplayName("should copy case details")
        void shouldCopyCaseDetails() {
            final CaseResource caseResource = new CaseResource(caseDetails, caseDataContent);

            assertAll(
                () -> assertThat(caseResource.getReference(), equalTo(REFERENCE.toString())),
                () -> assertThat(caseResource.getCreatedOn(), equalTo(CREATED_ON)),
                () -> assertThat(caseResource.getLastModifiedOn(), equalTo(LAST_MODIFIED_ON)),
                () -> assertThat(caseResource.getJurisdiction(), equalTo(JURISDICTION)),
                () -> assertThat(caseResource.getCaseType(), equalTo(CASE_TYPE)),
                () -> assertThat(caseResource.getState(), equalTo(STATE)),
                () -> assertThat(caseResource.getSecurityClassification(), equalTo(SECURITY_CLASSIFICATION)),
                () -> assertThat(caseResource.getData(), equalTo(DATA)),
                () -> assertThat(caseResource.getDataClassification(), equalTo(DATA_CLASSIFICATION)),
                () -> assertThat(caseResource.getAfterSubmitCallbackResponse(), equalTo(CALLBACK_BODY)),
                () -> assertThat(caseResource.getCallbackResponseStatusCode(), equalTo(AFTER_SUBMIT_CALLBACK_RESPONSE.getStatusCodeValue())),
                () -> assertThat(caseResource.getCallbackResponseStatus(), equalTo(CALLBACK_COMPLETED)),
                () -> assertThat(caseResource.getDeleteDraftResponseStatusCode(), equalTo(DELETE_DRAFT_RESPONSE.getStatusCodeValue())),
                () -> assertThat(caseResource.getDeleteDraftResponseStatus(), equalTo(DELETE_DRAFT_COMPLETED))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final CaseResource caseResource = new CaseResource(caseDetails, caseDataContent);

            Optional<Link> self = caseResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(LINK_SELF));
        }
    }

    @Nested
    @DisplayName("Create case")
    class CreateCase {

        private final String LINK_SELF = String.format("/case-types/%s/cases?ignore-warning=%s", CASE_TYPE, IGNORE_WARNING);

        @Test
        @DisplayName("should copy case details")
        void shouldCopyCaseDetails() {
            final CaseResource caseResource = new CaseResource(caseDetails, caseDataContent, IGNORE_WARNING);

            assertAll(
                () -> assertThat(caseResource.getReference(), equalTo(REFERENCE.toString())),
                () -> assertThat(caseResource.getCreatedOn(), equalTo(CREATED_ON)),
                () -> assertThat(caseResource.getLastModifiedOn(), equalTo(LAST_MODIFIED_ON)),
                () -> assertThat(caseResource.getJurisdiction(), equalTo(JURISDICTION)),
                () -> assertThat(caseResource.getCaseType(), equalTo(CASE_TYPE)),
                () -> assertThat(caseResource.getState(), equalTo(STATE)),
                () -> assertThat(caseResource.getSecurityClassification(), equalTo(SECURITY_CLASSIFICATION)),
                () -> assertThat(caseResource.getData(), equalTo(DATA)),
                () -> assertThat(caseResource.getDataClassification(), equalTo(DATA_CLASSIFICATION)),
                () -> assertThat(caseResource.getAfterSubmitCallbackResponse(), equalTo(CALLBACK_BODY)),
                () -> assertThat(caseResource.getCallbackResponseStatusCode(), equalTo(AFTER_SUBMIT_CALLBACK_RESPONSE.getStatusCodeValue())),
                () -> assertThat(caseResource.getCallbackResponseStatus(), equalTo(CALLBACK_COMPLETED)),
                () -> assertThat(caseResource.getDeleteDraftResponseStatusCode(), equalTo(DELETE_DRAFT_RESPONSE.getStatusCodeValue())),
                () -> assertThat(caseResource.getDeleteDraftResponseStatus(), equalTo(DELETE_DRAFT_COMPLETED))
            );
        }

        @Test
        @DisplayName("should link to itself")
        void shouldLinkToSelf() {
            final CaseResource caseResource = new CaseResource(caseDetails, caseDataContent, IGNORE_WARNING);

            Optional<Link> self = caseResource.getLink("self");
            assertThat(self.get().getHref(), equalTo(LINK_SELF));
        }
    }

    private CaseDetails aCaseDetails() {
        final CaseDetails caseDetails = new CaseDetails();

        caseDetails.setReference(REFERENCE);
        caseDetails.setCreatedDate(CREATED_ON);
        caseDetails.setLastModified(LAST_MODIFIED_ON);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setCaseTypeId(CASE_TYPE);
        caseDetails.setState(STATE);
        caseDetails.setSecurityClassification(SECURITY_CLASSIFICATION);
        caseDetails.setData(DATA);
        caseDetails.setDataClassification(DATA_CLASSIFICATION);
        caseDetails.setAfterSubmitCallbackResponseEntity(AFTER_SUBMIT_CALLBACK_RESPONSE);
        caseDetails.setDeleteDraftResponseEntity(DRAFT_ID, DELETE_DRAFT_RESPONSE);

        return caseDetails;
    }
}
