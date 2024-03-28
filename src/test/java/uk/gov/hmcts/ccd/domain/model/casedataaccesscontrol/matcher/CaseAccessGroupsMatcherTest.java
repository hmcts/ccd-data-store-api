package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.accessprofile.filter.BaseFilter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CaseAccessGroupsMatcherTest extends BaseFilter {

    private CaseAccessGroupsMatcher classUnderTest;
    private final String simpleGAjsonRequest = "{\n"
        + "  \"CaseAccessGroups\": [\n"
        + "    {\n"
        + "      \"id\": \"id1\",\n"
        + "      \"value\": {\n"
        + "        \"caseAccessGroupId\": \"caseGroupId1\",\n"
        + "        \"caseAccessGroupType\": \"caseAccessGroupType1\"\n"
        + "      }\n"
        + "    },\n"
        + "    {\n"
        + "      \"id\": \"id2\",\n"
        + "      \"value\": {\n"
        + "        \"caseAccessGroupId\": \"caseGroupId2\",\n"
        + "        \"caseAccessGroupType\": \"caseAccessGroupType2\"\n"
        + "      }\n"
        + "    }\n"
        + "  ]\n"
        + "}\n";

    @BeforeEach
    void setUp() {
        classUnderTest = new CaseAccessGroupsMatcher();
    }

    @Test
    void shouldMatchOnCaseAccessGroupId() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, Optional.of("caseGroupId1"));

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree(simpleGAjsonRequest));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchOnCaseAccessGroupIdWithEmptyRAValues() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, Optional.of("caseGroupId1"));

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree("{}"));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchOnCaseAccessGroupIdWithEmptyCaseValues() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, null);

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree(simpleGAjsonRequest));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldMatchOnCaseAccessGroupIdWithEmptyCaseAndRAValues() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, null);

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree("{}"));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertTrue(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchOnCaseGroupId() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, Optional.of("caseGroupId3"));

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree(simpleGAjsonRequest));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchOnCaseAccessGroupIdWithMissingItemId() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, Optional.of("caseGroupId1"));

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        String replacing = "\"id\": \"id1\",\n";
        String missing = simpleGAjsonRequest.replaceAll(replacing, "\n");

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree(missing));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchOnCaseAccessGroupIdWithMissingValue() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, Optional.of("caseGroupId1"));

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        String replacing = ",\n      \"value\": \\{\n        \"caseAccessGroupId\": \"caseGroupId1\",\n"
            + "        \"caseAccessGroupType\": \"caseAccessGroupType1\"\n      \\}\n";
        String missing = simpleGAjsonRequest.replaceAll(replacing, "\n");

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree(missing));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

    @Test
    void shouldNotMatchOnCaseAccessGroupIdWhenNotAnArray() throws JsonProcessingException {
        RoleAssignment roleAssignment = createRoleAssignment(Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE", null, null, null, null, Optional.of("caseGroupId1"));

        CaseDetails caseDetails = mockCaseDetails();
        ObjectMapper mapper = new ObjectMapper();

        String missing = "{\n\"caseAccessGroups\": \"\"}\n";

        Map<String, JsonNode> dataMap = JacksonUtils.convertValue(mapper.readTree(missing));

        when(caseDetails.getData()).thenReturn(dataMap);
        assertFalse(classUnderTest.matchAttribute(roleAssignment, caseDetails));
    }

}
