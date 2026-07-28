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

class CaseSearchPostRequestTest {

    private static final String SEARCH_LOG_TIMESTAMP_AS_TEXT = "2018-08-19T16:02:42.010Z";
    private static final String SEARCH_LOG_USER_ID = "1234";
    private static final String SEARCH_LOG_CASE_REFS = "100001,100002";
    private static final Clock fixedClock = Clock.fixed(Instant.parse(SEARCH_LOG_TIMESTAMP_AS_TEXT), ZoneOffset.UTC);
    private static final ZonedDateTime SEARCH_LOG_TIMESTAMP =
        ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);

    private CaseSearchPostRequest caseSearchPostRequest;

    private ZonedDateTime currentZonedDateTime;

    @Test
    void jsonContructionTestWithMultipleCaseRefs() throws JacksonException {

        final SearchLog searchLog = new SearchLog();
        searchLog.setUserId(SEARCH_LOG_USER_ID);
        searchLog.setCaseRefs(SEARCH_LOG_CASE_REFS);
        searchLog.setTimestamp(SEARCH_LOG_TIMESTAMP);

        caseSearchPostRequest = new CaseSearchPostRequest(searchLog);
        ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults()
            .changeDefaultVisibility(visibility ->
                visibility.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
            .build();

        String jsonRequest = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(caseSearchPostRequest);

        Map<String, JsonNode> value = JacksonUtils.convertValue(objectMapper.readTree(jsonRequest));

        assertNotNull(value);
        assertEquals(SEARCH_LOG_USER_ID, value.get("searchLog").get("userId").asString());
        assertEquals("100001", value.get("searchLog").get("caseRefs").get(0).asString());
        assertEquals("100002", value.get("searchLog").get("caseRefs").get(1).asString());
        assertEquals(SEARCH_LOG_TIMESTAMP_AS_TEXT, value.get("searchLog").get("timestamp").asString());
    }

    @Test
    void jsonContructionTestWithEmptyCaseRefs() throws JacksonException {
        final SearchLog searchLog = new SearchLog();
        searchLog.setUserId(SEARCH_LOG_USER_ID);
        searchLog.setCaseRefs(null);
        searchLog.setTimestamp(SEARCH_LOG_TIMESTAMP);

        caseSearchPostRequest = new CaseSearchPostRequest(searchLog);
        ObjectMapper objectMapper = JsonMapper.builderWithJackson2Defaults()
            .changeDefaultVisibility(visibility ->
                visibility.withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
            .build();

        String jsonRequest = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(caseSearchPostRequest);

        Map<String, JsonNode> value = JacksonUtils.convertValue(objectMapper.readTree(jsonRequest));

        assertNotNull(value);
        assertEquals(SEARCH_LOG_USER_ID, value.get("searchLog").get("userId").asString());
        assertEquals(0, value.get("searchLog").get("caseRefs").size());
        assertEquals(SEARCH_LOG_TIMESTAMP_AS_TEXT, value.get("searchLog").get("timestamp").asString());
    }
}
