package uk.gov.hmcts.ccd.domain.model.lau;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CaseSearchPostRequestTest {

    private static final String SEARCH_LOG_TIMESTAMP_AS_TEXT = "2018-08-19T16:02:42.010Z";
    private static final String SEARCH_LOG_USER_ID = "1234";
    private static final List<String> SEARCH_LOG_CASE_REFS = Arrays.asList("100001", "100002");
    private static final Clock fixedClock = Clock.fixed(Instant.parse(SEARCH_LOG_TIMESTAMP_AS_TEXT), ZoneOffset.UTC);
    private static final ZonedDateTime SEARCH_LOG_TIMESTAMP =
        ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);

    private CaseSearchPostRequest caseSearchPostRequest;

    private ZonedDateTime currentZonedDateTime;

    @Test
    void jsonContructionTestWithMultipleCaseRefs() throws JsonProcessingException {
        caseSearchPostRequest = new CaseSearchPostRequest(
            new SearchLog(SEARCH_LOG_USER_ID, SEARCH_LOG_CASE_REFS, SEARCH_LOG_TIMESTAMP));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        String jsonRequest = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(caseSearchPostRequest);

        Map<String, JsonNode> value = JacksonUtils.convertValue(objectMapper.readTree(jsonRequest));

        assertNotNull(value);
        assertEquals(SEARCH_LOG_USER_ID, value.get("searchLog").get("userId").asText());
        assertEquals(SEARCH_LOG_CASE_REFS.get(0), value.get("searchLog").get("caseRefs").get(0).asText());
        assertEquals(SEARCH_LOG_CASE_REFS.get(1), value.get("searchLog").get("caseRefs").get(1).asText());
        assertEquals(SEARCH_LOG_TIMESTAMP_AS_TEXT, value.get("searchLog").get("timestamp").asText());
    }

    @Test
    void jsonContructionTestWithEmptyCaseRefs() throws JsonProcessingException {
        caseSearchPostRequest = new CaseSearchPostRequest(
            new SearchLog(SEARCH_LOG_USER_ID, null, SEARCH_LOG_TIMESTAMP));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        String jsonRequest = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(caseSearchPostRequest);

        Map<String, JsonNode> value = JacksonUtils.convertValue(objectMapper.readTree(jsonRequest));

        assertNotNull(value);
        assertEquals(SEARCH_LOG_USER_ID, value.get("searchLog").get("userId").asText());
        assertEquals(0, value.get("searchLog").get("caseRefs").size());
        assertEquals(SEARCH_LOG_TIMESTAMP_AS_TEXT, value.get("searchLog").get("timestamp").asText());
    }
}
