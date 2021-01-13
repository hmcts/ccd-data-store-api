package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.newCaseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;

class DraftResponseToCaseDetailsBuilderTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String DRAFT_ID = "1";
    private static final String FULL_DRAFT_ID = "DRAFT1";
    private static final SecurityClassification SECURITY_CLASSIFICATION = SecurityClassification.PRIVATE;
    private static final String SECURITY_CLASSIFICATION_STRING = SECURITY_CLASSIFICATION.name();
    private static final Map<String, JsonNode> DATA = newCaseData()
        .withPair("testDataKey", JSON_NODE_FACTORY.textNode("testDataValue"))
        .build();
    private static final Map<String, JsonNode> DATA_CLASSIFICATION = newCaseData()
        .withPair("testClassificationKey", JSON_NODE_FACTORY.textNode("testClassificationValue"))
        .build();
    private static final String TOKEN = "testToken";
    private static final String EVENT_ID = "EVENT_ID";
    private static final String EVENT_DESCRIPTION = "Event description";
    private static final String EVENT_SUMMARY = "Event summary";
    private static final LocalDateTime CREATED = LocalDateTime.now();
    private static final LocalDateTime UPDATED = LocalDateTime.now().plus(5, ChronoUnit.MINUTES);
    private static final String EVENT_TRIGGER_ID = "EVENT_TRIGGER_ID";
    private static final String CASE_TYPE_ID = "CASE_TYPE_ID";
    private static final String JURISDICTION_ID = "JURISDICTION_ID";
    private static final String USER_ID = "USER_ID";
    private static final String TYPE = "CaseDataContent";

    private final DraftResponse draftResponse = newDraftResponse()
        .withId(DRAFT_ID)
        .withCreated(CREATED)
        .withUpdated(UPDATED)
        .withType(TYPE)
        .withDocument(newCaseDraft()
                          .withEventId(EVENT_TRIGGER_ID)
                          .withCaseTypeId(CASE_TYPE_ID)
                          .withJurisdictionId(JURISDICTION_ID)
                          .withUserId(USER_ID)
                          .withCaseDataContent(newCaseDataContent()
                                                   .withToken(TOKEN)
                                                   .withSecurityClassification(SECURITY_CLASSIFICATION_STRING)
                                                   .withDataClassification(DATA_CLASSIFICATION)
                                                   .withData(DATA)
                                                   .withIgnoreWarning(false)
                                                   .withEvent(anEvent()
                                                                  .withEventId(EVENT_ID)
                                                                  .withSummary(EVENT_SUMMARY)
                                                                  .withDescription(EVENT_DESCRIPTION)
                                                                  .build())
                                                   .build())
                          .build())
        .build();

    private final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder =
        new DraftResponseToCaseDetailsBuilder();

    @Test
    public void shouldBuildCaseDetailsFromDraftResponse() {
        CaseDetails caseDetails = draftResponseToCaseDetailsBuilder.build(draftResponse);

        assertAll(
            () -> assertThat(caseDetails.getJurisdiction(), is(JURISDICTION_ID)),
            () -> assertThat(caseDetails.getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(caseDetails.getSecurityClassification(), is(SECURITY_CLASSIFICATION)),
            () -> assertThat(caseDetails.getDataClassification(), is(DATA_CLASSIFICATION)),
            () -> assertThat(caseDetails.getData(), is(DATA)),
            () -> assertThat(caseDetails.getId(), is(FULL_DRAFT_ID)),
            () -> assertThat(caseDetails.getState(), is(nullValue())),
            () -> assertThat(caseDetails.getCreatedDate(), is(CREATED)),
            () -> assertThat(caseDetails.getLastModified(), is(UPDATED))
        );
    }

}
