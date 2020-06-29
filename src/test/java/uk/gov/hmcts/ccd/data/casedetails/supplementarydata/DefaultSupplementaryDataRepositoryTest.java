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
        Map<String, Object> organizationData = createRequestDataOrgA();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", organizationData);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getSupplementaryData();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationA", 32);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataColumnEmpty() {
        assumeDataInitialised();
        Map<String, Object> organizationData = createRequestData();
        supplementaryDataRepository.setSupplementaryData("1504259907353545", organizationData);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353545");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getSupplementaryData();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationB", 3);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetNewSupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        Map<String, Object> organizationData = createRequestData();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", organizationData);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getSupplementaryData();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationA", 10);
        validateResponseData(response, "organisationB", 3);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetMultipleEntrySupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        Map<String, Object> organizationData = createRequestWithMultipleEntries();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", organizationData);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getSupplementaryData();
        assertTrue(response.keySet().contains("orgs_assigned_users"));
        validateResponseData(response, "organisationA", 10);
        validateResponseData(response, "organisationB", 3);
        validateResponseData(response, "organisationC", 23);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void incrementSupplementaryData() {
        assumeDataInitialised();
        Map<String, Object> organizationData = createIncrementRequest(3);

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", organizationData);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getSupplementaryData();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationA", 13);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void decrementSupplementaryData() {
        assumeDataInitialised();
        Map<String, Object> organizationData = createIncrementRequest(-11);

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", organizationData);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getSupplementaryData();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationA", 0);
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddDataWhenIncrementCalledWhenSupplementaryDataEmpty() {
        assumeDataInitialised();
        Map<String, Object> organizationData = createRequestData();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353552", organizationData);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353552");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getSupplementaryData();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationB", 3);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddNewDataWhenIncrementCalledWhenParentPathExists() {
        assumeDataInitialised();
        Map<String, Object> organizationData = createRequestData();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", organizationData);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(response);
        Map<String, Object> responseMap = response.getSupplementaryData();
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
        Map<String, Object> responseMap = supplementaryData.getSupplementaryData();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
        validateResponseData(responseMap, "organisationA", 10);
    }

    private void assumeDataInitialised() {
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());
    }

    private Map<String, Object> createRequestData() {
        String jsonRequest = "{\n"
            + "\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationB\": 3\n"
            + "\t}\n"
            + "}";

        return convertData(jsonRequest);
    }

    private Map<String, Object> createRequestWithMultipleEntries() {
        String jsonRequest = "{\n"
            + "\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationB\": 3,\n"
            + "\t\t\"organisationC\": 23\n"
            + "\t}\n"
            + "}";

        return convertData(jsonRequest);
    }

    private void validateResponseData(Map<String, Object> response, String expectedKey, Object expectedValue) {
        Map<String, Object> childMap = (Map<String, Object> ) response.get("orgs_assigned_users");
        assertTrue(childMap.containsKey(expectedKey));
        assertEquals(expectedValue, childMap.get(expectedKey));
    }

    private Map<String, Object> createIncrementRequest(int value) {
        String jsonRequest = "{\n"
            + "\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationA\": " + value + "\n"
            + "\t}\n"
            + "}";

        return convertData(jsonRequest);
    }

    private Map<String, Object> createRequestDataOrgA() {
        String jsonRequest = "{\n"
            + "\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationA\": 32\n"
            + "\t}\n"
            + "}";

        return convertData(jsonRequest);
    }

    private Map<String, Object> convertData(String jsonRequest) {
        Map<String, Object> requestData;
        try {
            requestData = mapper.readValue(jsonRequest, Map.class);
        } catch (JsonProcessingException e) {
            requestData = new HashMap<>();
        }
        return requestData;
    }

}
