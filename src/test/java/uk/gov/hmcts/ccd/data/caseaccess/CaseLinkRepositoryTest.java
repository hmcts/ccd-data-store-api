package uk.gov.hmcts.ccd.data.caseaccess;

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
import static uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity.NON_STANDARD_LINK;
import static uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity.STANDARD_LINK;

public class CaseLinkRepositoryTest extends WireMockBaseTest {

    private static final String TEST_ADDRESS_BOOK_CASE = "TestAddressBookCase";

    @Inject
    private CaseLinkRepository caseLinkRepository;

    private CaseLinkEntity caseLinkEntity;

    @Before
    public void setup() {
        caseLinkEntity = new CaseLinkEntity(CASE_13_ID, CASE_14_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK);
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
        assertEquals(caseLinkEntity.getStandardLink(),
            savedEntity.getStandardLink());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveCaseLinkEntityWithStandardLinkTrue() {

        caseLinkEntity.setStandardLink(STANDARD_LINK);
        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);
        assertNotNull(savedEntity);
        assertNotNull(savedEntity.getCaseLinkPrimaryKey());
        assertEquals(caseLinkEntity.getCaseLinkPrimaryKey().getCaseId(),
            savedEntity.getCaseLinkPrimaryKey().getCaseId());
        assertEquals(caseLinkEntity.getCaseLinkPrimaryKey().getLinkedCaseId(),
            savedEntity.getCaseLinkPrimaryKey().getLinkedCaseId());
        assertEquals(caseLinkEntity.getCaseTypeId(),
            savedEntity.getCaseTypeId());
        assertEquals(caseLinkEntity.getStandardLink(),
            savedEntity.getStandardLink());
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
        caseLinkEntity = new CaseLinkEntity(1003L, CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK);

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveFailsIfLinkedCaseIdDoesNotExist() {
        caseLinkEntity = new CaseLinkEntity(CASE_01_ID, 999L, "TestAddressBookCase", NON_STANDARD_LINK);

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testDeleteAllByCaseReference() {

        // GIVEN
        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(CASE_01_ID, CASE_02_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(CASE_01_ID, CASE_03_ID, TEST_ADDRESS_BOOK_CASE, STANDARD_LINK),
            new CaseLinkEntity(CASE_02_ID, CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        // WHEN
        int deletedCount = caseLinkRepository.deleteAllByCaseReference(
            Long.parseLong(CASE_01_REFERENCE)
        );

        // THEN
        assertEquals(2, deletedCount);
        // check deleted
        assertFalse(caseLinkRepository.findById(caseLinkEntities.get(0).getCaseLinkPrimaryKey()).isPresent());
        assertFalse(caseLinkRepository.findById(caseLinkEntities.get(1).getCaseLinkPrimaryKey()).isPresent());
        // check remain (i.e. must only delete the case links belonging to the case reference supplied)
        assertTrue(caseLinkRepository.findById(caseLinkEntities.get(2).getCaseLinkPrimaryKey()).isPresent());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testInsertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId() {

        CaseLinkEntity.CaseLinkPrimaryKey pk = new CaseLinkEntity.CaseLinkPrimaryKey();
        pk.setCaseId(CASE_19_ID);
        pk.setLinkedCaseId(CASE_21_ID);

        assertFalse(caseLinkRepository.findById(pk).isPresent());

        caseLinkRepository.insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(Long.parseLong(CASE_19_REFERENCE),
                                                                                    Long.parseLong(CASE_21_REFERENCE),
                                                                                    "test",
                                                                                    NON_STANDARD_LINK);

        Optional<CaseLinkEntity> savedCaseLinkEntity = caseLinkRepository.findById(pk);
        assertTrue(savedCaseLinkEntity.isPresent());
        assertEquals(CASE_19_ID, savedCaseLinkEntity.get().getCaseLinkPrimaryKey().getCaseId());
        assertEquals(CASE_21_ID, savedCaseLinkEntity.get().getCaseLinkPrimaryKey().getLinkedCaseId());
        assertEquals("test", savedCaseLinkEntity.get().getCaseTypeId());
        assertEquals(NON_STANDARD_LINK, savedCaseLinkEntity.get().getStandardLink());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testFindAllByCaseReference() {
        final List<Long> linkedCaseIds = List.of(CASE_02_ID, CASE_03_ID, CASE_04_ID);

        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(0), TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(1), TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(2), TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK)
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
