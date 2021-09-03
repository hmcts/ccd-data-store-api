package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseDataIssueLoggerTest {

    private static final long CASE_ID = 123456L;
    private static final long CASE_REFERENCE = 987654L;
    private static final String JURISDICTION_ID = "EMPLOYMENT";
    private static final String CASE_TYPE_ID = "TestCaseType";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<>() {};

    private CaseDetails caseDetails;

    @Mock
    private ApplicationParams applicationParams;

    private CaseDataIssueLogger caseDataIssueLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(this.applicationParams.getCaseDataIssueLoggingJurisdictions())
            .thenReturn(singletonList(JURISDICTION_ID));

        caseDataIssueLogger = new CaseDataIssueLogger(applicationParams);
        caseDetails = new CaseDetails();
        caseDetails.setId(valueOf(CASE_ID));
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState("Create");
    }

    @Test
    public void logAnyDataIssuesWithNullCaseDetails() {
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void logAnyDataIssuesWithEmptyCaseDetails() {
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(new CaseDetails(), new CaseDetails());
        assertTrue(result.isEmpty());
    }

    @Test
    public void logAnyDataIssuesWithNullData() {
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(caseDetails, caseDetails);
        assertTrue(result.isEmpty());
    }

    @Test
    public void logAnyDataIssuesWithEmptyData() {
        caseDetails.setData(new HashMap<>());
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(caseDetails, caseDetails);
        assertTrue(result.isEmpty());
    }

    @Test
    public void logAnyDataIssuesWithNewEmptyCollection() throws IOException {
        String jsonData = "{"
            + "\"positionType\": \"Listed for a preliminary hearing(CM)\","
            + "\"jurCodesCollection\": ["
            + "  {"
            + "    \"id\": \"70d869ac-b21b-4faf-a609-d9fe60a0f507\","
            + "    \"value\": {}"
            + "  },"
            + "  {"
            + "    \"id\": \"618b2cb3-0707-4dc6-827f-8f3c9e68e2a6\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": \"YXU\""
            + "    }"
            + "  }"
            + "],"
            + "\"claimantRepresentedQuestion\": \"Yes\""
            + "}";

        caseDetails.setData(convertToDataMap(jsonData));
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(null, caseDetails);
        assertEquals(1, result.size());
        assertEquals("jurCodesCollection", result.get(0));
    }

    @Test
    public void logAnyDataIssuesWithExistingEmptyCollection() throws IOException {
        String jsonData = "{"
            + "\"positionType\": \"Listed for a preliminary hearing(CM)\","
            + "\"jurCodesCollection\": ["
            + "  {"
            + "    \"id\": \"70d869ac-b21b-4faf-a609-d9fe60a0f507\","
            + "    \"value\": {}"
            + "  },"
            + "  {"
            + "    \"id\": \"618b2cb3-0707-4dc6-827f-8f3c9e68e2a6\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": \"YXU\""
            + "    }"
            + "  }"
            + "],"
            + "\"claimantRepresentedQuestion\": \"Yes\""
            + "}";

        caseDetails.setData(convertToDataMap(jsonData));
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(caseDetails, caseDetails);
        assertTrue(result.isEmpty());
    }

    @Test
    public void logAnyDataIssuesWithoutNewEmptyCollection() throws IOException {
        String jsonData = "{"
            + "\"positionType\": \"Listed for a preliminary hearing(CM)\","
            + "\"jurCodesCollection\": ["
            + "  {"
            + "    \"id\": \"70d869ac-b21b-4faf-a609-d9fe60a0f507\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": \"DAG\""
            + "    }"
            + "  },"
            + "  {"
            + "    \"id\": \"618b2cb3-0707-4dc6-827f-8f3c9e68e2a6\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": \"YXU\""
            + "    }"
            + "  }"
            + "],"
            + "\"claimantRepresentedQuestion\": \"Yes\""
            + "}";

        caseDetails.setData(convertToDataMap(jsonData));
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(null, caseDetails);
        assertTrue(result.isEmpty());
    }

    @Test
    public void logAnyDataIssuesWithoutExistingEmptyCollection() throws IOException {
        String jsonData = "{"
            + "\"positionType\": \"Listed for a preliminary hearing(CM)\","
            + "\"jurCodesCollection\": ["
            + "  {"
            + "    \"id\": \"70d869ac-b21b-4faf-a609-d9fe60a0f507\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": \"DAG\""
            + "    }"
            + "  },"
            + "  {"
            + "    \"id\": \"618b2cb3-0707-4dc6-827f-8f3c9e68e2a6\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": \"YXU\""
            + "    }"
            + "  }"
            + "],"
            + "\"claimantRepresentedQuestion\": \"Yes\""
            + "}";

        caseDetails.setData(convertToDataMap(jsonData));
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(caseDetails, caseDetails);
        assertTrue(result.isEmpty());
    }

    @Test
    public void logAnyDataIssuesWithException() throws IOException {
        String jsonData = "{"
            + "\"positionType\": \"Listed for a preliminary hearing(CM)\","
            + "\"jurCodesCollection\": ["
            + "  {"
            + "    \"id\": \"70d869ac-b21b-4faf-a609-d9fe60a0f507\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": \"DAG\""
            + "    }"
            + "  },"
            + "  {"
            + "    \"id\": \"618b2cb3-0707-4dc6-827f-8f3c9e68e2a6\","
            + "    \"value\": {"
            + "      \"juridictionCodesList\": null"
            + "    }"
            + "  }"
            + "],"
            + "\"claimantRepresentedQuestion\": \"Yes\""
            + "}";

        caseDetails.setData(convertToDataMap(jsonData));
        List<String> result = caseDataIssueLogger.logAnyDataIssuesIn(caseDetails, caseDetails);
        assertTrue(result.isEmpty());
    }

    private Map<String, JsonNode> convertToDataMap(String jsonData) throws IOException {
        return MAPPER.convertValue(MAPPER.readTree(jsonData), STRING_JSON_MAP);
    }
}
