package uk.gov.hmcts.ccd.data.caseaccess;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import javax.inject.Inject;
import java.util.List;
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
        caseLinkEntity = new CaseLinkEntity(13L, 14L, TEST_ADDRESS_BOOK_CASE);
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
        caseLinkEntity = new CaseLinkEntity(1003L, 1L, TEST_ADDRESS_BOOK_CASE);

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testSaveFailsIfLinkedCaseIdDoesNotExist() {
        caseLinkEntity = new CaseLinkEntity(1L, 999L, "TestAddressBookCase");

        assertThrows(DataIntegrityViolationException.class, () -> caseLinkRepository.save(caseLinkEntity));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testDeleteByCaseReferenceAndLinkedCaseReference() {

        CaseLinkEntity savedEntity = caseLinkRepository.save(caseLinkEntity);

        int deletedCount = caseLinkRepository.deleteByCaseReferenceAndLinkedCaseReference(
            1504259907353651L,
            1504259907353598L);

        assertFalse(caseLinkRepository.findById(savedEntity.getCaseLinkPrimaryKey()).isPresent());

        assertEquals(1, deletedCount);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testInsertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId() {

        CaseLinkEntity.CaseLinkPrimaryKey pk = new CaseLinkEntity.CaseLinkPrimaryKey();
        pk.setCaseId(19L);
        pk.setLinkedCaseId(20L);

        assertFalse(caseLinkRepository.findById(pk).isPresent());

        caseLinkRepository.insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(1601933818308168L,
                                                                            9816494993793181L,
                                                                            "test");

        assertTrue(caseLinkRepository.findById(pk).isPresent());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void testFindAllByCaseReference() {
        final List<Long> linkedCaseIds = List.of(2L, 3L, 4L);

        final List<CaseLinkEntity> caseLinkEntities = List.of(
            new CaseLinkEntity(1L, linkedCaseIds.get(0), TEST_ADDRESS_BOOK_CASE),
            new CaseLinkEntity(1L, linkedCaseIds.get(1), TEST_ADDRESS_BOOK_CASE),
            new CaseLinkEntity(1L, linkedCaseIds.get(2), TEST_ADDRESS_BOOK_CASE)
        );

        caseLinkRepository.saveAll(caseLinkEntities);

        final List<CaseLinkEntity> allByCaseReference = caseLinkRepository.findAllByCaseReference(1504259907353529L);

        assertEquals(caseLinkEntities.size(), allByCaseReference.size());

        final List<Long> foundLinkedCaseIds = allByCaseReference.stream()
            .filter(caseLinkEntity -> caseLinkEntity.getCaseTypeId().equals(TEST_ADDRESS_BOOK_CASE)
                && caseLinkEntity.getCaseLinkPrimaryKey().getCaseId().equals(1L))
            .map(cle -> cle.getCaseLinkPrimaryKey().getLinkedCaseId())
            .collect(Collectors.toList());

        assertTrue(foundLinkedCaseIds.containsAll(linkedCaseIds));
    }

}
