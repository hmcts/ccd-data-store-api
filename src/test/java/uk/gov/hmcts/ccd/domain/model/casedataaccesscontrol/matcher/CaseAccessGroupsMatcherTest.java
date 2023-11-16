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
        + "  \"caseAccessGroups\": [\n"
        + "    {\n"
        + "      \"id\": \"ffaec4ae-e7dc-43f1-862b-1b85041bc36d\",\n"
        + "      \"value\": {\n"
        + "        \"caseAccessGroupId\": \"caseGroupId1\",\n"
        + "        \"caseGroupType\": \"caseGroupType1\"\n"
        + "      }\n"
        + "    },\n"
        + "    {\n"
        + "      \"id\": \"46410c2a-acda-43e0-bb2e-cd5b1663c616\",\n"
        + "      \"value\": {\n"
        + "        \"caseAccessGroupId\": \"caseGroupId2\",\n"
        + "        \"caseGroupType\": \"caseGroupType2\"\n"
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
    void shouldMatchOnCaseAccessGroupIdWithEmptyValues() throws JsonProcessingException {
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
}
