package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDataResource;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataBuilder.newCaseData;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@DisplayName("CaseDataValidatorControllerTest")
class CaseDataValidatorControllerTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String PAGE_ID = "PageId";
    private static final boolean IGNORE_WARNING = false;

    private static final Event EVENT = anEvent().build();
    private static final Map<String, JsonNode> DATA = newCaseData()
        .withPair("data", JSON_NODE_FACTORY.objectNode().set("aField", JSON_NODE_FACTORY.textNode("aValue")))
        .build();
    private static final JsonNode DATA_NODE = JacksonUtils.convertValueJsonNode(DATA);
    public static final Map<String, JsonNode> UNWRAPPED_DATA = newCaseData()
        .withPair("aField", JSON_NODE_FACTORY.textNode("aValue"))
        .build();
    private static final JsonNode UNWRAPPED_DATA_NODE = JacksonUtils.convertValueJsonNode(UNWRAPPED_DATA);
    private static final String TOKEN = "JwtToken";
    private static final CaseDataContent EVENT_DATA = newCaseDataContent().withEvent(EVENT).withData(DATA)
        .withToken(TOKEN).build();

    @Mock
    private ValidateCaseFieldsOperation validateCaseFieldsOperation;
    @Mock
    private MidEventCallback midEventCallback;

    @InjectMocks
    private CaseDataValidatorController caseDataValidatorController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, EVENT_DATA)).thenReturn(DATA);
        when(midEventCallback.invoke(CASE_TYPE_ID, EVENT_DATA, PAGE_ID)).thenReturn(DATA_NODE);
    }

    @Nested
    @DisplayName("POST /case-types/{caseTypeId}/validate")
    class PostCaseDataValidate {

        @Test
        @DisplayName("should return 200 when case data valid")
        void shouldPassIfCaseDataValid() {
            final ResponseEntity<CaseDataResource> response =
                caseDataValidatorController.validate(CASE_TYPE_ID, PAGE_ID, EVENT_DATA);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getData(), is(UNWRAPPED_DATA_NODE))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(validateCaseFieldsOperation.validateCaseDetails(CASE_TYPE_ID, EVENT_DATA))
                .thenThrow(RuntimeException.class);

            assertThrows(Exception.class, () -> caseDataValidatorController.validate(CASE_TYPE_ID, PAGE_ID,
                EVENT_DATA));
        }
    }
}
