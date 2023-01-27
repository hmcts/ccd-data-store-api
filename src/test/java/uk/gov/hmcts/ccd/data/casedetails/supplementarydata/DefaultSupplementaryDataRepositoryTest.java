package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DefaultSupplementaryDataRepositoryTest extends WireMockBaseTest {

    private static final int NUMBER_OF_CASES = 6;
    private JdbcTemplate template;

    private final SupplementaryDataRepository supplementaryDataRepository;

    DefaultSupplementaryDataRepositoryTest(@Qualifier("default")
                                               SupplementaryDataRepository supplementaryDataRepository) {
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    @BeforeEach
    void setUp() {
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldReplaceExistingSupplementaryData() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationA", 32);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(32, response.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataColumnEmpty() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353545",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353545",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationB"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetNewSupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationB"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353529",
                Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(10, response.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetMultipleEntrySupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationC",
            23);

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationC"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationC"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353529",
                Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(10, response.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void incrementSupplementaryData() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationA",
            3);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(13, responseMap.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void decrementSupplementaryData() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationA",
            -11);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(-1, responseMap.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldNotDecrementSupplementaryDataWhenSupplementaryDataDoNotHaveTheParent() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907311111",
            "orgs_assigned_users.organisationA",
            -1);

        assertThrows(ServiceException.class,
            () -> supplementaryDataRepository.findSupplementaryData("1504259907311111",
            Sets.newHashSet("orgs_assigned_users.organisationA")));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddDataWhenIncrementCalledWhenSupplementaryDataEmpty() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353552",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353552",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.containsKey("orgs_assigned_users.organisationB"));
        assertEquals(3, responseMap.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddNewDataWhenIncrementCalledWhenParentPathExists() {
        assumeDataInitialised();
        supplementaryDataRepository.incrementSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.containsKey("orgs_assigned_users.organisationB"));
        assertEquals(3, responseMap.get("orgs_assigned_users.organisationB"));

        response = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(response);
        responseMap = response.getResponse();
        assertTrue(responseMap.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(10, responseMap.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void findSupplementaryData() {
        assumeDataInitialised();
        final SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353529", null);
        assertNotNull(supplementaryData);
        Map<String, Object> responseMap = supplementaryData.getResponse();
        assertTrue(responseMap.containsKey("orgs_assigned_users"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataHasOtherParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907311111",
            "orgs_assigned_users.organisationC",
            23);

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907311111",
                Sets.newHashSet("orgs_assigned_users.organisationC"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationC"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907311111", Sets.newHashSet("HMCTSServiceId"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("HMCTSServiceId"));
        assertEquals("BBA3", response.get("HMCTSServiceId"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataHasOtherParent1() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529",
            "HMCTSServiceId", "BBA3");

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353529",
                Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(10, response.get("orgs_assigned_users.organisationA"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907353529", Sets.newHashSet("HMCTSServiceId"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("HMCTSServiceId"));
        assertEquals("BBA3", response.get("HMCTSServiceId"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataHasSameParentAndOtherParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907322222",
            "orgs_assigned_users.organisationC",
            23);

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907322222",
                Sets.newHashSet("orgs_assigned_users.organisationC"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationC"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907322222", Sets.newHashSet("HMCTSServiceId"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("HMCTSServiceId"));
        assertEquals("BBA3", response.get("HMCTSServiceId"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907322222",
                Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(15, response.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldIncrementSupplementaryDataWhenSupplementaryDataHasOtherParent() {
        assumeDataInitialised();
        supplementaryDataRepository.incrementSupplementaryData("1504259907311111",
            "orgs_assigned_users.organisationC",
            1);

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907311111",
                Sets.newHashSet("orgs_assigned_users.organisationC"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationC"));
        assertEquals(1, response.get("orgs_assigned_users.organisationC"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907311111", Sets.newHashSet("HMCTSServiceId"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("HMCTSServiceId"));
        assertEquals("BBA3", response.get("HMCTSServiceId"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
        {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldIncrementSupplementaryDataWhenSupplementaryDataHasSameParentAndOtherParent() {
        assumeDataInitialised();
        supplementaryDataRepository.incrementSupplementaryData("1504259907322222",
            "orgs_assigned_users.organisationB",
            2);

        SupplementaryData supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907322222",
                Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationB"));
        assertEquals(5, response.get("orgs_assigned_users.organisationB"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907322222", Sets.newHashSet("HMCTSServiceId"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("HMCTSServiceId"));
        assertEquals("BBA3", response.get("HMCTSServiceId"));

        supplementaryData =
            supplementaryDataRepository.findSupplementaryData("1504259907322222",
                Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(supplementaryData);
        response = supplementaryData.getResponse();
        assertTrue(response.containsKey("orgs_assigned_users.organisationA"));
        assertEquals(15, response.get("orgs_assigned_users.organisationA"));
    }

    private void assumeDataInitialised() {
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals(NUMBER_OF_CASES, resultList.size(), "Incorrect data initiation");
    }
}
