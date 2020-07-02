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
        SupplementaryDataUpdateRequest request = createRequestDataOrgA();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", request);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529", request);
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationA"));
        assertEquals(32, response.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataColumnEmpty() {
        assumeDataInitialised();
        SupplementaryDataUpdateRequest request = createRequestData("$set");
        supplementaryDataRepository.setSupplementaryData("1504259907353545", request);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353545", request);
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetNewSupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        SupplementaryDataUpdateRequest request = createRequestData("$set");
        supplementaryDataRepository.setSupplementaryData("1504259907353529", request);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529", request);
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetMultipleEntrySupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        SupplementaryDataUpdateRequest request = createRequestWithMultipleEntries();
        supplementaryDataRepository.setSupplementaryData("1504259907353529", request);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529", request);
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationC"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void incrementSupplementaryData() {
        assumeDataInitialised();

        SupplementaryDataUpdateRequest request = createIncrementRequest(3);
        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", request);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529", request);
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationA"));
        assertEquals(13, responseMap.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void decrementSupplementaryData() {
        assumeDataInitialised();

        SupplementaryDataUpdateRequest request = createIncrementRequest(-11);
        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", request);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529", request);
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationA"));
        assertEquals(0, responseMap.get("orgs_assigned_users.organisationA"));
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddDataWhenIncrementCalledWhenSupplementaryDataEmpty() {
        assumeDataInitialised();

        SupplementaryDataUpdateRequest request = createRequestData("$inc");
        supplementaryDataRepository.incrementSupplementaryData("1504259907353552", request);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353552", request);
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, responseMap.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddNewDataWhenIncrementCalledWhenParentPathExists() {
        assumeDataInitialised();

        SupplementaryDataUpdateRequest request = createRequestData("$inc");
        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", request);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529", request);
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, responseMap.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void findSupplementaryData() {
        assumeDataInitialised();
        final SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529", null);
        assertNotNull(supplementaryData);
        Map<String, Object> responseMap = supplementaryData.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
    }

    private void assumeDataInitialised() {
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());
    }

    private SupplementaryDataUpdateRequest createRequestData(String operationName) {
        String jsonRequest = "{\n"
            + "\t\"" + operationName + "\": {\n"
            + "\t\t\"orgs_assigned_users.organisationB\": 3\n"
            + "\t}\n"
            + "}";


        return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
    }

    private SupplementaryDataUpdateRequest createRequestWithMultipleEntries() {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationB\": 3,\n"
            + "\t\t\"orgs_assigned_users.organisationC\": 23\n"
            + "\t}\n"
            + "}";

        return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
    }

    private SupplementaryDataUpdateRequest createIncrementRequest(int value) {
        String jsonRequest = "{\n"
            + "\t\"$inc\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": " + value + "\n"
            + "\t}\n"
            + "}";

        return new SupplementaryDataUpdateRequest(convertData(jsonRequest));
    }

    private SupplementaryDataUpdateRequest createRequestDataOrgA() {
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users.organisationA\": 32\n"
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
