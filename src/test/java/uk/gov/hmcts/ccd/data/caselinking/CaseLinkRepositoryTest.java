package uk.gov.hmcts.ccd.data.caselinking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.NON_STANDARD_LINK;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.STANDARD_LINK;

class CaseLinkRepositoryTest extends WireMockBaseTest {

    private static final String TEST_ADDRESS_BOOK_CASE = "TestAddressBookCase";

    @Inject
    private CaseLinkRepository caseLinkRepository;

    private CaseLinkEntity caseLinkEntity;

    private final Map<Long, Long> caseLinkIdToReferenceMap = new HashMap<>();

    @BeforeEach
    void setup() {
        caseLinkEntity = new CaseLinkEntity(CASE_13_ID, CASE_14_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK);

        caseLinkIdToReferenceMap.put(CASE_02_ID, parseLong(CASE_02_REFERENCE));
        caseLinkIdToReferenceMap.put(CASE_03_ID, parseLong(CASE_03_REFERENCE));
        caseLinkIdToReferenceMap.put(CASE_04_ID, parseLong(CASE_04_REFERENCE));
        caseLinkIdToReferenceMap.put(CASE_13_ID, parseLong(CASE_13_REFERENCE));
        caseLinkIdToReferenceMap.put(CASE_14_ID, parseLong(CASE_14_REFERENCE));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testSaveCaseLinkEntity() {

        // WHEN
        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);

        // THEN
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
    void testSaveCaseLinkEntityWithStandardLinkTrue() {

        // GIVEN
        caseLinkEntity.setCaseTypeId(TEST_ADDRESS_BOOK_CASE);
        caseLinkEntity.setStandardLink(STANDARD_LINK);

        // WHEN
        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);

        // THEN
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
    void testDeleteCaseLinkEntity() {

        // GIVEN
        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);

        // WHEN
        caseLinkRepository.delete(savedEntity);

        // THEN
        assertFalse(caseLinkRepository.findById(savedEntity.getCaseLinkPrimaryKey()).isPresent());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testFindCaseLinkEntityById() {

        // WHEN
        caseLinkRepository.save(caseLinkEntity);

        // THEN
        assertTrue(caseLinkRepository.findById(caseLinkEntity.getCaseLinkPrimaryKey()).isPresent());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testSaveFailsIfCaseIdDoesNotExist() {

        // WHEN
        caseLinkEntity = new CaseLinkEntity(1003L, CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK);

        // THEN
        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testSaveFailsIfLinkedCaseIdDoesNotExist() {

        // WHEN
        caseLinkEntity = new CaseLinkEntity(CASE_01_ID, 999L, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK);

        // THEN
        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testDeleteAllByCaseReference() {

        // GIVEN
        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(CASE_01_ID, CASE_02_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(CASE_01_ID, CASE_03_ID, TEST_ADDRESS_BOOK_CASE, STANDARD_LINK),
            new CaseLinkEntity(CASE_02_ID, CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        // WHEN
        int deletedCount = caseLinkRepository.deleteAllByCaseReference(
            parseLong(CASE_01_REFERENCE)
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
    void testInsertUsingCaseReferences() {

        // GIVEN
        CaseLinkEntity.CaseLinkPrimaryKey pk = new CaseLinkEntity.CaseLinkPrimaryKey();
        pk.setCaseId(CASE_19_ID);
        pk.setLinkedCaseId(CASE_21_ID);

        assertFalse(caseLinkRepository.findById(pk).isPresent());

        // WHEN
        caseLinkRepository.insertUsingCaseReferences(parseLong(CASE_19_REFERENCE),
                                                     parseLong(CASE_21_REFERENCE),
                                                     NON_STANDARD_LINK);

        // THEN
        Optional<CaseLinkEntity>  savedCaseLinkEntity = caseLinkRepository.findById(pk);
        assertTrue(savedCaseLinkEntity.isPresent());
        assertEquals(CASE_19_ID, savedCaseLinkEntity.get().getCaseLinkPrimaryKey().getCaseId());
        assertEquals(CASE_21_ID, savedCaseLinkEntity.get().getCaseLinkPrimaryKey().getLinkedCaseId());
        assertNotNull(savedCaseLinkEntity.get().getCaseTypeId());
        assertEquals(NON_STANDARD_LINK, savedCaseLinkEntity.get().getStandardLink());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testFindAllByCaseReference() {

        // GIVEN
        final List<Long> linkedCaseIds = List.of(CASE_02_ID, CASE_03_ID, CASE_04_ID);

        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(0), TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(1), TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(CASE_01_ID, linkedCaseIds.get(2), TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        long caseReference = parseLong(CASE_01_REFERENCE);

        // WHEN
        final List<CaseLinkEntity> allByCaseReference = caseLinkRepository.findAllByCaseReference(caseReference);

        // THEN
        assertEquals(caseLinkEntities.size(), allByCaseReference.size());

        final List<Long> foundLinkedCaseIds = allByCaseReference.stream()
            .filter(caseLinkEntity -> caseLinkEntity.getCaseTypeId().equals(TEST_ADDRESS_BOOK_CASE)
                && caseLinkEntity.getCaseLinkPrimaryKey().getCaseId().equals(CASE_01_ID))
            .map(cle -> cle.getCaseLinkPrimaryKey().getLinkedCaseId())
            .collect(Collectors.toList());

        assertTrue(foundLinkedCaseIds.containsAll(linkedCaseIds));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testFindCaseReferencesByLinkedCaseReferenceAndStandardLinkTrue() {

        // GIVEN
        final List<Long> linkedCaseIds = List.of(CASE_02_ID, CASE_03_ID, CASE_04_ID);

        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(linkedCaseIds.get(0), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(1), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(2), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, STANDARD_LINK)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        // WHEN
        final List<Long> caseReferences = caseLinkRepository.findCaseReferencesByLinkedCaseReferenceAndStandardLink(
            parseLong(CASE_01_REFERENCE), STANDARD_LINK);

        // THEN
        assertEquals(caseLinkEntities.size(), caseReferences.size());
        assertTrue(caseReferences.containsAll(List.of(caseLinkIdToReferenceMap.get(linkedCaseIds.get(0)),
            caseLinkIdToReferenceMap.get(linkedCaseIds.get(1)),
            caseLinkIdToReferenceMap.get(linkedCaseIds.get(2)))));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testFindCaseReferencesByLinkedCaseReferenceAndStandardLinkFalse() {

        // GIVEN
        final List<Long> linkedCaseIds = List.of(CASE_02_ID, CASE_03_ID, CASE_04_ID);

        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(linkedCaseIds.get(0), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(1), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(2), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        // WHEN
        final List<Long> caseReferences = caseLinkRepository.findCaseReferencesByLinkedCaseReferenceAndStandardLink(
            parseLong(CASE_01_REFERENCE), STANDARD_LINK);

        // THEN
        assertEquals(0, caseReferences.size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testFindCaseReferencesByLinkedCaseReferenceAndStandardLink() {

        // GIVEN
        final List<Long> linkedCaseIds = List.of(CASE_02_ID, CASE_03_ID, CASE_04_ID, CASE_13_ID, CASE_14_ID);

        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(linkedCaseIds.get(0), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(1), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(2), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(3), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, STANDARD_LINK),
            new CaseLinkEntity(linkedCaseIds.get(4), CASE_01_ID, TEST_ADDRESS_BOOK_CASE, NON_STANDARD_LINK)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        // WHEN
        final List<Long> caseReferences = caseLinkRepository.findCaseReferencesByLinkedCaseReferenceAndStandardLink(
            parseLong(CASE_01_REFERENCE), STANDARD_LINK);

        // THEN
        // assert only find the two that use STANDARD_LINK
        assertEquals(2, caseReferences.size());
        assertTrue(caseReferences.containsAll(List.of(caseLinkIdToReferenceMap.get(linkedCaseIds.get(1)),
            caseLinkIdToReferenceMap.get(linkedCaseIds.get(3)))));
    }

}
