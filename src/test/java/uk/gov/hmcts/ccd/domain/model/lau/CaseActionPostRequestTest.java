package uk.gov.hmcts.ccd.domain.model.lau;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CaseActionPostRequestTest {

    private static final String ACTION_LOG_TIMESTAMP_AS_TEXT = "2018-08-19T16:02:42.010Z";
    private static final String ACTION_LOG_USER_ID = "1234";
    private static final String ACTION_LOG_CASE_REF = "100001";
    private static final String ACTION_LOG_CASE_ACTION = "VIEW";
    private static final String ACTION_LOG_CASE_JURISDICTION_ID = "Probate";
    private static final String ACTION_LOG_CASE_TYPE_ID = "Caveat";

    private static final Clock fixedClock = Clock.fixed(Instant.parse(ACTION_LOG_TIMESTAMP_AS_TEXT), ZoneOffset.UTC);
    private static final ZonedDateTime ACTION_LOG_TIMESTAMP =
        ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);

    private CaseActionPostRequest caseActionPostRequest;

    private ZonedDateTime currentZonedDateTime;

    @Test
    void jsonContructionTestWithMultipleCaseRefs() throws JacksonException {
        caseActionPostRequest = new CaseActionPostRequest(new ActionLog(
            ACTION_LOG_USER_ID,
            ACTION_LOG_CASE_ACTION,
            ACTION_LOG_CASE_REF,
            ACTION_LOG_CASE_JURISDICTION_ID,
            ACTION_LOG_CASE_TYPE_ID,
            ACTION_LOG_TIMESTAMP));

        ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults()
            .changeDefaultVisibility(visibility ->
                visibility.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
            .build();

        String jsonRequest = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(caseActionPostRequest);

        Map<String, JsonNode> value = JacksonUtils.convertValue(objectMapper.readTree(jsonRequest));

        assertNotNull(value);
        assertEquals(ACTION_LOG_USER_ID, value.get("actionLog").get("userId").asString());
        assertEquals(ACTION_LOG_CASE_REF, value.get("actionLog").get("caseRef").asString());
        assertEquals(ACTION_LOG_CASE_ACTION, value.get("actionLog").get("caseAction").asString());
        assertEquals(ACTION_LOG_CASE_TYPE_ID, value.get("actionLog").get("caseTypeId").asString());
        assertEquals(ACTION_LOG_CASE_JURISDICTION_ID, value.get("actionLog").get("caseJurisdictionId").asString());
        assertEquals(ACTION_LOG_TIMESTAMP_AS_TEXT, value.get("actionLog").get("timestamp").asString());
    }

}
