package uk.gov.hmcts.ccd.data.caselinking;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaseLinkRepositoryTest extends WireMockBaseTest {

    private static final String TEST_ADDRESS_BOOK_CASE = "TestAddressBookCase";

    @Inject
    private CaseLinkRepository caseLinkRepository;

    private CaseLinkEntity caseLinkEntity;

    @Before
    public void setup() {
        caseLinkEntity = new CaseLinkEntity(CASE_13_ID, CASE_14_ID, TEST_ADDRESS_BOOK_CASE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveCaseLinkEntity() {

        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);
        assertNotNull(savedEntity);
        assertNotNull(savedEntity.getCaseLinkPrimaryKey());
        assertEquals(caseLinkEntity.getCaseLinkPrimaryKey().getCaseId(),
            savedEntity.getCaseLinkPrimaryKey().getCaseId());
        assertEquals(caseLinkEntity.getCaseLinkPrimaryKey().getLinkedCaseId(),
            savedEntity.getCaseLinkPrimaryKey().getLinkedCaseId());
        assertEquals(caseLinkEntity.getCaseTypeId(),
            savedEntity.getCaseTypeId());
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
        caseLinkEntity = new CaseLinkEntity(1003L, CASE_01_ID, TEST_ADDRESS_BOOK_CASE);

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveFailsIfLinkedCaseIdDoesNotExist() {
        caseLinkEntity = new CaseLinkEntity(CASE_01_ID, 999L, "TestAddressBookCase");

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testDeleteByCaseReferenceAndLinkedCaseReference() {

        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);

        int deletedCount = caseLinkRepository.deleteByCaseReferenceAndLinkedCaseReference(
            Long.parseLong(CASE_13_REFERENCE),
            Long.parseLong(CASE_14_REFERENCE)
        );

        assertFalse(caseLinkRepository.findById(savedEntity.getCaseLinkPrimaryKey()).isPresent());

        assertEquals(1, deletedCount);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testInsertUsingCaseReferences() {

        CaseLinkEntity.CaseLinkPrimaryKey pk = new CaseLinkEntity.CaseLinkPrimaryKey();
        pk.setCaseId(CASE_19_ID);
        pk.setLinkedCaseId(CASE_21_ID);

        assertFalse(caseLinkRepository.findById(pk).isPresent());

        caseLinkRepository.insertUsingCaseReferences(Long.parseLong(CASE_19_REFERENCE),
                                                     Long.parseLong(CASE_21_REFERENCE));

        Optional<CaseLinkEntity>  caseLinkEntity = caseLinkRepository.findById(pk);
        assertTrue(caseLinkEntity.isPresent());
        assertNotNull(caseLinkEntity.get().getCaseTypeId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testFindAllByCaseReference() {
        final List<Long> linkedCaseIds = List.of(CASE_02_ID, CASE_03_ID, CASE_04_ID);

        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(0), TEST_ADDRESS_BOOK_CASE),
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(1), TEST_ADDRESS_BOOK_CASE),
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(2), TEST_ADDRESS_BOOK_CASE)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        long caseReference = Long.parseLong(CASE_01_REFERENCE);
        final List<CaseLinkEntity> allByCaseReference = caseLinkRepository.findAllByCaseReference(caseReference);

        assertEquals(caseLinkEntities.size(), allByCaseReference.size());

        final List<Long> foundLinkedCaseIds = allByCaseReference.stream()
            .filter(caseLinkEntity -> caseLinkEntity.getCaseTypeId().equals(TEST_ADDRESS_BOOK_CASE)
                && caseLinkEntity.getCaseLinkPrimaryKey().getCaseId().equals(CASE_01_ID))
            .map(cle -> cle.getCaseLinkPrimaryKey().getLinkedCaseId())
            .collect(Collectors.toList());

        assertTrue(foundLinkedCaseIds.containsAll(linkedCaseIds));
    }

}
