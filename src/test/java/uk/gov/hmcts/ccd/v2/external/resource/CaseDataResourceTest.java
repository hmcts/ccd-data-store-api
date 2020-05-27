package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.newCaseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@DisplayName("CaseDataResource")
class CaseDataResourceTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String PAGE_ID = "pageId";
    private static final Map<String, JsonNode> DATA = newCaseData()
        .withPair("data", JSON_NODE_FACTORY.objectNode().set("aField", JSON_NODE_FACTORY.textNode("aValue")))
        .build();
    public static final Map<String, JsonNode> UNWRAPPED_DATA = newCaseData()
        .withPair("aField", JSON_NODE_FACTORY.textNode("aValue"))
        .build();
    private static final JsonNode UNWRAPPED_DATA_NODE = JacksonUtils.convertValueJsonNode(UNWRAPPED_DATA);
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().withData(DATA).build();

    private final String linkSelfForCaseData = String.format("/case-types/%s/validate?pageId=pageId", CASE_TYPE_ID);

    @Test
    @DisplayName("should copy case data unwrapped")
    void shouldCopyUnwrappedCaseDataContent() {
        final CaseDataResource caseDataResource = new CaseDataResource(CASE_DATA_CONTENT, CASE_TYPE_ID, PAGE_ID);

        assertAll(
            () -> assertThat(caseDataResource.getData(), equalTo(UNWRAPPED_DATA_NODE))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final CaseDataResource caseDataResource = new CaseDataResource(CASE_DATA_CONTENT, CASE_TYPE_ID, PAGE_ID);

        Optional<Link> self = caseDataResource.getLink("self");
        assertThat(self.get().getHref(), equalTo(linkSelfForCaseData));
    }

}
