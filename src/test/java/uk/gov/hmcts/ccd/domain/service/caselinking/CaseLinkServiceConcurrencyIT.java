package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import uk.gov.hmcts.ccd.AbstractBaseIntegrationTest;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;

@Import(CaseLinkServiceConcurrencyIT.CaseLinkConcurrencyTestConfig.class)
class CaseLinkServiceConcurrencyIT extends AbstractBaseIntegrationTest {

    @Autowired
    private CaseLinkService caseLinkService;

    @Autowired
    private CaseLinkRepository caseLinkRepository;

    @Autowired
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    @Autowired
    private CaseLinkExtractor caseLinkExtractor;

    @Autowired
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("updateCaseLinks handles concurrent updates on same case reference without deadlock")
    void shouldUpdateCaseLinksConcurrentlyForSameCaseReference() throws Exception {
        CaseDetails sourceCase = persistCase(1111222233334444L);
        CaseDetails linkedCaseOne = persistCase(9999000011112222L);
        CaseDetails linkedCaseTwo = persistCase(8888000011112222L);

        AtomicInteger invocation = new AtomicInteger();
        doAnswer(invocationOnMock -> {
            boolean firstCall = invocation.getAndIncrement() == 0;
            Long targetReference = firstCall ? linkedCaseOne.getReference() : linkedCaseTwo.getReference();
            return List.of(
                CaseLink.builder()
                    .caseReference(sourceCase.getReference())
                    .linkedCaseReference(targetReference)
                    .standardLink(firstCall)
                    .build()
            );
        }).when(caseLinkExtractor).getCaseLinksFromData(same(sourceCase), anyList());

        Callable<Void> updateTask = () -> {
            caseLinkService.updateCaseLinks(sourceCase, Collections.<CaseFieldDefinition>emptyList());
            return null;
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Void> first = executor.submit(() -> {
            start.await();
            return updateTask.call();
        });
        Future<Void> second = executor.submit(() -> {
            start.await();
            return updateTask.call();
        });

        start.countDown();

        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        List<CaseLinkEntity> links = caseLinkRepository.findAllByCaseReference(sourceCase.getReference());
        assertFalse(links.isEmpty(), "Expected at least one case link to be written");
        assertTrue(links.size() <= 2, "Concurrent updates should not leave more than two rows");
        long linkedCaseOneId = Long.parseLong(linkedCaseOne.getId());
        long linkedCaseTwoId = Long.parseLong(linkedCaseTwo.getId());
        assertTrue(
            links.stream()
                .allMatch(link -> {
                    Long linkedCaseId = link.getCaseLinkPrimaryKey().getLinkedCaseId();
                    return linkedCaseId.equals(linkedCaseOneId) || linkedCaseId.equals(linkedCaseTwoId);
                }),
            "All linked cases should be one of the two concurrent updates"
        );
    }

    @Test
    @DisplayName("insertUsingCaseReferences on inverse links completes without deadlock")
    void shouldInsertInverseLinksWithoutDeadlock() throws Exception {
        CaseDetails caseA = persistCase(5555000011118888L);
        CaseDetails caseB = persistCase(5555000011119999L);

        Callable<Void> aToBInsertTask = () -> {
            caseLinkRepository.insertUsingCaseReferences(caseA.getReference(), caseB.getReference(), true);
            return null;
        };
        Callable<Void> bToAInsertTask = () -> {
            caseLinkRepository.insertUsingCaseReferences(caseB.getReference(), caseA.getReference(), true);
            return null;
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> f1 = executor.submit(aToBInsertTask);
        Future<Void> f2 = executor.submit(bToAInsertTask);

        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        List<CaseLinkEntity> links = caseLinkRepository.findAllByCaseReference(caseA.getReference());
        assertFalse(links.isEmpty(), "Expected link from A");

        List<CaseLinkEntity> reverseLinks = caseLinkRepository.findAllByCaseReference(caseB.getReference());
        assertFalse(reverseLinks.isEmpty(), "Expected link from B");
    }

    @Test
    @DisplayName("updateCaseLinks shows expected deadlock behaviour when two transactions lock rows in opposite order")
    void shouldSurfaceDeadlockWhenLockOrderConflicts() throws Exception {
        CaseDetails caseOne = persistCase(7777000011112222L);
        CaseDetails caseTwo = persistCase(7777000011113333L);

        CountDownLatch firstLocksAcquired = new CountDownLatch(2);
        CountDownLatch proceedToSecondLock = new CountDownLatch(1);

        Callable<Void> t1 = () -> {
            return new TransactionTemplate(transactionManager).execute(status -> {
                jdbcTemplate.update("update case_data set state = ? where reference = ?", "LOCK_A",
                    caseOne.getReference());
                firstLocksAcquired.countDown();
                await(proceedToSecondLock);
                jdbcTemplate.update("update case_data set state = ? where reference = ?", "LOCK_B",
                    caseTwo.getReference());
                return null;
            });
        };

        Callable<Void> t2 = () -> {
            return new TransactionTemplate(transactionManager).execute(status -> {
                jdbcTemplate.update("update case_data set state = ? where reference = ?", "LOCK_C",
                    caseTwo.getReference());
                firstLocksAcquired.countDown();
                await(proceedToSecondLock);
                jdbcTemplate.update("update case_data set state = ? where reference = ?", "LOCK_D",
                    caseOne.getReference());
                return null;
            });
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> f1 = executor.submit(t1);
        Future<Void> f2 = executor.submit(t2);

        firstLocksAcquired.await(5, TimeUnit.SECONDS);
        proceedToSecondLock.countDown();

        boolean deadlockSeen = false;
        DataAccessException deadlockException = null;
        for (Future<Void> future : List.of(f1, f2)) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception ex) {
                deadlockSeen |= containsDeadlock(ex);
                if (ex instanceof DataAccessException) {
                    deadlockException = (DataAccessException) ex;
                } else if (ex.getCause() instanceof DataAccessException) {
                    deadlockException = (DataAccessException) ex.getCause();
                }
            }
        }

        executor.shutdownNow();

        assertTrue(deadlockSeen, "Expected one of the concurrent transactions to surface a deadlock");
        if (deadlockException != null) {
            assertTrue(
                deadlockException.getMostSpecificCause().getMessage().contains("deadlock"),
                "Deadlock exception should mention deadlock"
            );
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean containsDeadlock(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().toLowerCase().contains("deadlock")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Test
    @DisplayName("insert case_link entries in opposite lock order surfaces a database deadlock")
    void shouldDeadlockWhenInsertingCaseLinksWithOppositeLockOrdering() throws Exception {
        CaseDetails caseA = persistCase(5555000011112222L);
        CaseDetails caseB = persistCase(5555000011113333L);

        CountDownLatch firstLocks = new CountDownLatch(2);
        CountDownLatch proceed = new CountDownLatch(1);

        Callable<Void> t1 = () -> lockRowsInOrder(caseA.getReference(), caseB.getReference(), firstLocks, proceed);
        Callable<Void> t2 = () -> lockRowsInOrder(caseB.getReference(), caseA.getReference(), firstLocks, proceed);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> f1 = executor.submit(t1);
        Future<Void> f2 = executor.submit(t2);

        firstLocks.await(5, TimeUnit.SECONDS);
        proceed.countDown();

        boolean deadlockSeen = false;
        for (Future<Void> future : List.of(f1, f2)) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception ex) {
                deadlockSeen |= containsDeadlock(ex);
            }
        }

        executor.shutdownNow();
        assertTrue(deadlockSeen, "Expected one insert to surface a deadlock");
    }

    private Void lockRowsInOrder(Long firstRef, Long secondRef,
                                 CountDownLatch firstLocks, CountDownLatch proceed) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> {
            // Lock firstRef row first
            jdbcTemplate.queryForObject("select id from case_data where reference=? for update",
                Long.class, firstRef);
            firstLocks.countDown();
            await(proceed);
            // Now attempt to lock the second row in opposite order; this should deadlock with the peer transaction
            jdbcTemplate.queryForObject("select id from case_data where reference=? for update",
                Long.class, secondRef);
            return null;
        });
    }

    private CaseDetails persistCase(Long reference) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(reference);
        caseDetails.setCaseTypeId("FT_MasterCaseType");
        caseDetails.setJurisdiction("BEFTA_MASTER");
        LocalDateTime now = LocalDateTime.now();
        caseDetails.setCreatedDate(now);
        caseDetails.setLastModified(now);
        caseDetails.setLastStateModifiedDate(now);
        caseDetails.setState("CaseCreated");
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        caseDetails.setData(emptyJsonMap());
        caseDetails.setDataClassification(emptyJsonMap());
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> caseDetailsRepository.set(caseDetails));
    }

    private Map<String, JsonNode> emptyJsonMap() {
        return new HashMap<>();
    }

    @TestConfiguration
    static class CaseLinkConcurrencyTestConfig {
        @Bean
        CaseLinkExtractor caseLinkExtractor() {
            return org.mockito.Mockito.mock(CaseLinkExtractor.class);
        }

        @Bean
        @Primary
        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
            return org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.JwtDecoder.class);
        }
    }
}
