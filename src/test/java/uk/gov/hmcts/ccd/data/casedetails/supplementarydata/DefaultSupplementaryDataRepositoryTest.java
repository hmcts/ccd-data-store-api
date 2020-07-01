package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

class DefaultSupplementaryDataRepositoryTest extends WireMockBaseTest {

    private static final int NUMBER_OF_CASES = 4;
    private JdbcTemplate template;

    @Inject
    @Qualifier("default")
    private SupplementaryDataRepository supplementaryDataRepository;

    @BeforeEach
    void setUp() {
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldReplaceExistingSupplementaryData() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", createRequestDataOrgA());

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationA", 32);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataColumnEmpty() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353545", createRequestData("$set"));

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353545");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationB", 3);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetNewSupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", createRequestData("$set"));

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationA", 10);
        validateResponseData(response, "organisationB", 3);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetMultipleEntrySupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", createRequestWithMultipleEntries());

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationA", 10);
        validateResponseData(response, "organisationB", 3);
        validateResponseData(response, "organisationC", 23);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void incrementSupplementaryData() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", createIncrementRequest(3));

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationA", 13);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void decrementSupplementaryData() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", createIncrementRequest(-11));

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationA", 0);
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddDataWhenIncrementCalledWhenSupplementaryDataEmpty() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353552", createRequestData("$inc"));

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353552");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationB", 3);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddNewDataWhenIncrementCalledWhenParentPathExists() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529",  createRequestData("$inc"));

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationA", 10);
        validateResponseData(responseMap, "organisationB", 3);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void findSupplementaryData() {
        assumeDataInitialised();
        final SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        Map<String, Object> responseMap = supplementaryData.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationA", 10);
    }

    private void assumeDataInitialised() {
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());
    }

    private SupplementaryDataUpdateRequest createRequestData(String operationName) {
        String jsonRequest = "{\n"
            + "\t\"" + operationName + "\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationB\": 3\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";

        return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
    }

    private SupplementaryDataUpdateRequest createRequestWithMultipleEntries() {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationB\": 3,\n"
            + "\t\t\"organisationC\": 23\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";

        return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
    }

    private void validateResponseData(Map<String, Object> response, String expectedKey, Object expectedValue) {
        Map<String, Object> childMap = (Map<String, Object> ) response.get("orgs_assigned_users");
        assertTrue(childMap.containsKey(expectedKey));
        assertEquals(expectedValue, childMap.get(expectedKey));
    }

    private SupplementaryDataUpdateRequest createIncrementRequest(int value) {
        String jsonRequest = "{\n"
            + "\t\"$inc\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationA\": " + value + "\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";

        return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
    }

    private SupplementaryDataUpdateRequest createRequestDataOrgA() {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationA\": 32\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";
        return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
    }

    private Map<String, Map<String, Object>> convertData(String jsonRquest) {
        Map<String, Map<String, Object>> requestData;
        try {
            requestData = mapper.readValue(jsonRquest, Map.class);
        } catch (JsonProcessingException e) {
            requestData = new HashMap<>();
        }
        return requestData;
    }
}
