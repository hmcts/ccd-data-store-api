package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.v2.external.resource.StartTriggerResource;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("StartTriggerController")
class StartTriggerControllerTest {
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String EVENT_TRIGGER_ID = "createCase";
    private static final boolean IGNORE_WARNING = false;
    private static final String TOKEN = "TOKEN";
    private static final CaseDetails CASE_DETAILS = new CaseDetails();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    @Mock
    private StartEventOperation startEventOperation;

    @InjectMocks
    private StartTriggerController startTriggerController;

    private StartEventTrigger startEventTrigger = new StartEventTrigger();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        Map<String, JsonNode> data = Maps.newHashMap();
        data.put("dataKey1", JSON_NODE_FACTORY.textNode("dataValue1"));
        CASE_DETAILS.setData(data);
        Map<String, JsonNode> dataClassification = Maps.newHashMap();
        dataClassification.put("classKey1", JSON_NODE_FACTORY.textNode("classValue1"));
        CASE_DETAILS.setDataClassification(dataClassification);
        startEventTrigger.setCaseDetails(CASE_DETAILS);
        startEventTrigger.setToken(TOKEN);
        startEventTrigger.setEventId(EVENT_TRIGGER_ID);

        when(startEventOperation.triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenReturn(startEventTrigger);
    }

    @Nested
    @DisplayName("GET /case-types/{caseTypeId}/trigger/{triggerId}")
    class GetStartTrigger {

        @Test
        @DisplayName("should return 200 when start trigger found")
        void startTriggerFound() {
            final ResponseEntity<StartTriggerResource> response = startTriggerController.getStartTrigger(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseDetails(), is(CASE_DETAILS)),
                () -> assertThat(response.getBody().getEventId(), is(EVENT_TRIGGER_ID)),
                () -> assertThat(response.getBody().getToken(), is(TOKEN))
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(startEventOperation.triggerStartForCaseType(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING)).thenThrow(Exception.class);

            assertThrows(Exception.class, () -> startTriggerController.getStartTrigger(CASE_TYPE_ID, EVENT_TRIGGER_ID, IGNORE_WARNING));
        }


    }
}
