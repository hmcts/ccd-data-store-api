package uk.gov.hmcts.ccd.data.caseaccess;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaseLinkRepositoryTest extends WireMockBaseTest {

    @Inject
    private CaseLinkRepository caseLinkRepository;

    private CaseLinkEntity caseLinkEntity;

    @Before
    public void setup() {
        caseLinkEntity = new CaseLinkEntity(13L, 14L, "TestAddressBookCase");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveCaseLinkEntity() {
        assertNotNull(caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testDeleteCaseLinkEntity() {
        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);

        caseLinkRepository.delete(savedEntity);

        assertFalse(caseLinkRepository.findById(savedEntity.getCaseLinkPrimaryKey()).isPresent());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testFindCaseLinkEntityById() {
        caseLinkRepository.save(caseLinkEntity);

        assertTrue(caseLinkRepository.findById(caseLinkEntity.getCaseLinkPrimaryKey()).isPresent());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveFailsIfCaseIdDoesNotExist() {
        caseLinkEntity = new CaseLinkEntity(1003L, 1L, "TestAddressBookCase");

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveFailsIfLinkedCaseIdDoesNotExist() {
        caseLinkEntity = new CaseLinkEntity(1L, 999L, "TestAddressBookCase");

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }
}
