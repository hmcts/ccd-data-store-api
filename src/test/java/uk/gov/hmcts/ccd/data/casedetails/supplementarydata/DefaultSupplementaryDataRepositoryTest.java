package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.google.common.collect.Sets;
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
        supplementaryDataRepository.setSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationA", 32);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationA"));
        assertEquals(32, response.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetSupplementaryDataWhenSupplementaryDataColumnEmpty() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353545",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353545",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetNewSupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldSetMultipleEntrySupplementaryDataWhenSupplementaryDataHasSameParent() {
        assumeDataInitialised();
        supplementaryDataRepository.setSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationC",
            23);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationC"));
        assertNotNull(supplementaryData);
        Map<String, Object> response = supplementaryData.getResponse();
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationC"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void incrementSupplementaryData() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationA",
            3);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationA"));
        assertEquals(13, responseMap.get("orgs_assigned_users.organisationA"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void decrementSupplementaryData() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationA",
            -11);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationA"));
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationA"));
        assertEquals(-1, responseMap.get("orgs_assigned_users.organisationA"));
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddDataWhenIncrementCalledWhenSupplementaryDataEmpty() {
        assumeDataInitialised();

        supplementaryDataRepository.incrementSupplementaryData("1504259907353552",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353552",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
        assertNotNull(response);
        Map<String, Object> responseMap = response.getResponse();
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, responseMap.get("orgs_assigned_users.organisationB"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void shouldAddNewDataWhenIncrementCalledWhenParentPathExists() {
        assumeDataInitialised();
        supplementaryDataRepository.incrementSupplementaryData("1504259907353529",
            "orgs_assigned_users.organisationB",
            3);

        SupplementaryData response = supplementaryDataRepository.findSupplementaryData("1504259907353529",
            Sets.newHashSet("orgs_assigned_users.organisationB"));
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
}
