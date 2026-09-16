package uk.gov.hmcts.ccd.decentralised.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DecentralisedCaseEventTest {

    private final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

    @Test
    void shouldDeserializeDecentralisedEventPayload() throws Exception {
        String payload = """
            {
              "case_details_before": null,
              "case_details": null,
              "event_details": {
                "case_type": "E2E",
                "event_id": "create-test-application",
                "event_name": "Create test case",
                "description": "description",
                "summary": "summary"
              },
              "resolved_ttl": null,
              "internal_case_id": 1,
              "start_revision": 2,
              "merge_revision": 3
            }
            """;

        DecentralisedCaseEvent event = mapper.readValue(payload, DecentralisedCaseEvent.class);

        assertThat(event.getEventDetails()).isEqualTo(DecentralisedEventDetails.builder()
            .caseType("E2E")
            .eventId("create-test-application")
            .eventName("Create test case")
            .description("description")
            .summary("summary")
            .build());
        assertThat(event.getInternalCaseId()).isEqualTo(1L);
        assertThat(event.getStartRevision()).isEqualTo(2L);
        assertThat(event.getMergeRevision()).isEqualTo(3L);
    }
}
