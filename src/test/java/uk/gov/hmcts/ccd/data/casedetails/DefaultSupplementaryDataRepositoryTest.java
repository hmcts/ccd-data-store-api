package uk.gov.hmcts.ccd.data.casedetails;

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

    private static final int NUMBER_OF_CASES = 3;
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
    public void setSupplementaryData() {
        assumeDataInitialised();
        Map<String, Object> organizationData = new HashMap<>();
        organizationData.put("orgs_assigned_users,organisationB", "3");
        supplementaryDataRepository.setSupplementaryData("1504259907353529", organizationData);

        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        assertTrue(supplementaryData.getSupplementaryData().keySet().contains("orgs_assigned_users"));
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void incrementSupplementaryData() {
        assumeDataInitialised();
        Map<String, Object> organizationData = new HashMap<>();
        organizationData.put("orgs_assigned_users,organisationA", "2");
        SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        supplementaryDataRepository.incrementSupplementaryData("1504259907353529", organizationData);

        supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        assertTrue(supplementaryData.getSupplementaryData().keySet().contains("orgs_assigned_users"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_supplementary_data.sql"})
    public void findSupplementaryData() {
        assumeDataInitialised();
        final SupplementaryData supplementaryData = supplementaryDataRepository.findSupplementaryData("1504259907353529");
        assertNotNull(supplementaryData);
        assertTrue(supplementaryData.getSupplementaryData().keySet().contains("orgs_assigned_users"));
    }

    private void assumeDataInitialised() {
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());
    }

}
