package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.newCaseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@DisplayName("CaseDocumentResource")
class CaseDocumentResourceTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2_1";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";
    private static final String CASE_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_URL = "http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_NAME = "Sample_document.txt";
    private static final String DOCUMENT_TYPE = "Document";

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PAGE_ID = "pageId";
    private static final Map<String, JsonNode> DATA = newCaseData()
        .withPair("data", JSON_NODE_FACTORY.objectNode().set("aField", JSON_NODE_FACTORY.textNode("aValue")))
        .build();
    public static final Map<String, JsonNode> UNWRAPPED_DATA = newCaseData()
        .withPair("aField", JSON_NODE_FACTORY.textNode("aValue"))
        .build();
    private static final JsonNode UNWRAPPED_DATA_NODE =  MAPPER.convertValue(UNWRAPPED_DATA, JsonNode.class);
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().withData(DATA).build();

    private final String linkSelfForCaseData = String.format("/case-types/%s/validate?pageId=pageId", CASE_TYPE_ID);
    private final String linkSelfForCaseDocument = String.format("/cases/%s/documents/%s", CASE_REFERENCE, CASE_DOCUMENT_ID);
    private final CaseDocumentMetadata caseDocumentMetadata = CaseDocumentMetadata.builder()
        .caseId(CASE_REFERENCE)
            .caseTypeId(CASE_TYPE_ID)
            .jurisdictionId(JURISDICTION_ID)
            .document(CaseDocument.builder()
                .id(CASE_DOCUMENT_ID)
                .url(DOCUMENT_URL)
                .name(DOCUMENT_NAME)
                .type(DOCUMENT_TYPE)
                .permissions(Arrays.asList(Permission.READ, Permission.UPDATE))
        .build())
        .build();

    @Test
    @DisplayName("should copy case document metadata unwrapped")
    void shouldCopyUnwrappedCaseDocumentMetadataContent() {
        final CaseDocumentResource result = new CaseDocumentResource(CASE_REFERENCE, CASE_DOCUMENT_ID, caseDocumentMetadata);

        assertAll(
            () -> assertThat(result.getDocumentMetadata(), equalTo(caseDocumentMetadata))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final CaseDocumentResource result = new CaseDocumentResource(CASE_REFERENCE, CASE_DOCUMENT_ID, caseDocumentMetadata);

        Optional<Link> self = result.getLink("self");
        assertThat(self.get().getHref(), equalTo(linkSelfForCaseDocument));
    }

}
