package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DefaultSupplementaryDataRepositoryTest extends WireMockBaseTest {

    private static final int NUMBER_OF_CASES = 8;
    private JdbcTemplate template;

    @Inject
    @Qualifier("default")
    private uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository supplementaryDataRepository;

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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationA"));
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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, response.get("orgs_assigned_users.organisationB"));
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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationC"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));
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
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationA"));
        assertEquals(13, responseMap.get("orgs_assigned_users.organisationA"));
    }

    @Nested
    @DisplayName("find casese with invalid supplementary data test")
    class FindCasesWithInvalidSupplementaryData {

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
            {"classpath:sql/insert_cases_supplementary_data.sql"})
        public void findCasesWithSupplementaryDataHavingHmctsServiceIdButNoOrgsAssignedUsers() {
            assumeDataInitialised();

            LocalDateTime from = LocalDateTime.of(2016, 1, 1, 8, 55);
            LocalDateTime to = LocalDateTime.of(2020, 1, 1, 8, 55);
            Integer limit = 5;
            List<String> response = supplementaryDataRepository
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(from, Optional.of(to), limit);
            assertNotNull(response);
            assertEquals(3, response.size());
            assertTrue(response.contains("1504259907311111"));
            assertTrue(response.contains("1504259907311112"));
            assertTrue(response.contains("1504259907311113"));
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
            {"classpath:sql/insert_cases_supplementary_data.sql"})
        public void findCasesFilteredByDateFrom() {
            assumeDataInitialised();

            LocalDateTime from = LocalDateTime.of(2016, 9, 24, 20, 41);
            Integer limit = 10;
            List<String> response = supplementaryDataRepository
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(from, Optional.empty(), limit);
            assertNotNull(response);
            assertEquals(1, response.size());
            assertEquals("1504259907311113", response.get(0));
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
            {"classpath:sql/insert_cases_supplementary_data.sql"})
        public void findCasesFilteredByLimit() {
            assumeDataInitialised();

            LocalDateTime from = LocalDateTime.of(2016, 1, 1, 8, 55);
            Integer limit = 1;
            List<String> response = supplementaryDataRepository
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(from, Optional.empty(), limit);
            assertNotNull(response);
            assertEquals(1, response.size());
            assertEquals("1504259907311111", response.get(0));
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
            {"classpath:sql/insert_cases_supplementary_data.sql"})
        public void findCasesFilteredWithEmptyDateTo() {
            assumeDataInitialised();

            LocalDateTime from = LocalDateTime.of(2016, 1, 1, 8, 55);
            Integer limit = 100;
            List<String> response = supplementaryDataRepository
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(from, Optional.empty(), limit);
            assertNotNull(response);
            assertEquals(3, response.size());
            assertTrue(response.contains("1504259907311111"));
            assertTrue(response.contains("1504259907311112"));
            assertTrue(response.contains("1504259907311113"));
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts =
            {"classpath:sql/insert_cases_supplementary_data.sql"})
        public void findCasesFilteredByDateTo() {
            assumeDataInitialised();

            LocalDateTime from = LocalDateTime.of(2016, 1, 1, 8, 55);
            LocalDateTime to = LocalDateTime.of(2016, 9, 24, 20, 41);
            Integer limit = 5;
            List<String> response = supplementaryDataRepository
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(from, Optional.of(to), limit);
            assertNotNull(response);
            assertEquals(2, response.size());
            assertTrue(response.contains("1504259907311111"));
            assertTrue(response.contains("1504259907311112"));
        }
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
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationA"));
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
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationB"));
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
        assertTrue(responseMap.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(3, responseMap.get("orgs_assigned_users.organisationB"));
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
        assertTrue(responseMap.keySet().contains("orgs_assigned_users"));
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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationC"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));
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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationC"));
        assertEquals(23, response.get("orgs_assigned_users.organisationC"));
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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationC"));
        assertEquals(1, response.get("orgs_assigned_users.organisationC"));
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
        assertTrue(response.keySet().contains("orgs_assigned_users.organisationB"));
        assertEquals(5, response.get("orgs_assigned_users.organisationB"));
    }



    private void assumeDataInitialised() {
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals(NUMBER_OF_CASES, resultList.size(), "Incorrect data initiation");
    }
}
