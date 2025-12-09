package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://${DATA_STORE_DB_HOST:localhost}:${DATA_STORE_DB_PORT:5050}/"
        + "${DATA_STORE_DB_NAME:ccd_data}?stringtype=unspecified",
    "spring.datasource.username=${DATA_STORE_DB_USERNAME:ccd}",
    "spring.datasource.password=${DATA_STORE_DB_PASSWORD:ccd}",
    "spring.datasource.hikari.maximum-pool-size=5",
    "spring.datasource.hikari.minimum-idle=2",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=false",
    "spring.security.oauth2.client.provider.oidc.issuer-uri=${IDAM_OIDC_URL:http://localhost:5000/o}"
})
class CaseLinkServiceExternalDbDeadlockIT {

    @Autowired
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CaseLinkService caseLinkService;

    @Autowired
    private CaseLinkRepository caseLinkRepository;

    @MockitoBean
    private CaseLinkExtractor caseLinkExtractor;

    @Test
    @DisplayName("deadlock reproduced against external Postgres when inserting case_link rows with inverse locks")
    void shouldSurfaceDeadlockAgainstExternalDb() throws Exception {
        Assumptions.assumeTrue(externalDbAvailable(), "External Postgres not available for deadlock test");

        long refA = 7000000000000000L + System.currentTimeMillis() % 1000000;
        long refB = refA + 1;
        CaseDetails caseA = persistCase(refA);
        CaseDetails caseB = persistCase(refB);

        CountDownLatch firstLocks = new CountDownLatch(2);
        CountDownLatch proceed = new CountDownLatch(1);

        Callable<Void> t1 = () -> insertLink(caseA.getReference(), caseB.getReference(), firstLocks, proceed);
        Callable<Void> t2 = () -> insertLink(caseB.getReference(), caseA.getReference(), firstLocks, proceed);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> f1 = executor.submit(t1);
        Future<Void> f2 = executor.submit(t2);

        firstLocks.await(5, TimeUnit.SECONDS);
        proceed.countDown();

        boolean deadlockSeen = false;
        for (Future<Void> future : new Future[]{f1, f2}) {
            try {
                future.get(15, TimeUnit.SECONDS);
            } catch (Exception ex) {
                deadlockSeen |= containsDeadlock(ex);
            }
        }

        executor.shutdownNow();
        assertTrue(deadlockSeen, "Expected one of the inserts to surface a deadlock against external DB");
    }

    @Test
    @DisplayName("CaseLinkService updateCaseLinks avoids deadlock against external DB")
    void shouldAvoidDeadlockWhenUpdatingCaseLinksAgainstExternalDb() throws Exception {
        Assumptions.assumeTrue(externalDbAvailable(), "External Postgres not available for deadlock test");

        long refBase = 8000000000000000L + System.currentTimeMillis() % 1000000;
        CaseDetails sourceCase = persistCase(refBase);
        CaseDetails linkedA = persistCase(refBase + 1);
        CaseDetails linkedB = persistCase(refBase + 2);

        AtomicInteger invocation = new AtomicInteger();
        doAnswer(invocationOnMock -> {
            boolean firstCall = invocation.getAndIncrement() == 0;
            Long linkedRef = firstCall ? linkedA.getReference() : linkedB.getReference();
            return List.of(CaseLink.builder()
                .caseReference(sourceCase.getReference())
                .linkedCaseReference(linkedRef)
                .standardLink(true)
                .build());
        }).when(caseLinkExtractor).getCaseLinksFromData(same(sourceCase), anyList());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Void> updateTask = () -> {
            start.await();
            caseLinkService.updateCaseLinks(sourceCase, List.of());
            return null;
        };

        Future<Void> f1 = executor.submit(updateTask);
        Future<Void> f2 = executor.submit(updateTask);

        start.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        List<uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity> links =
            caseLinkRepository.findAllByCaseReference(sourceCase.getReference());
        assertFalse(links.isEmpty(), "Expected case links to be written without deadlock");
    }

    private Void insertLink(Long firstRef, Long secondRef,
                            CountDownLatch firstLocks, CountDownLatch proceed) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> {
            // lock the first row to establish opposite lock ordering between the two transactions
            jdbcTemplate.queryForObject("select id from case_data where reference=? for update", Long.class, firstRef);
            firstLocks.countDown();
            await(proceed);
            // attempt to lock the second row; this should deadlock with the peer transaction
            jdbcTemplate.queryForObject("select id from case_data where reference=? for update", Long.class, secondRef);
            // if we got here, insert the link (one side may still succeed)
            jdbcTemplate.update(
                "insert into case_link (case_id, linked_case_id, case_type_id, standard_link) values ("
                    + "(select id from case_data cd where cd.reference=?), "
                    + "(select id from case_data cd where cd.reference=?), "
                    + "(select case_type_id from case_data cd where cd.reference=?), "
                    + "true)",
                firstRef, secondRef, secondRef
            );
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
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> caseDetailsRepository.set(caseDetails));
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
            if (current instanceof DataAccessException) {
                String msg = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
                if (msg.contains("deadlock") || msg.contains("sqlstate 40p01")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, JsonNode> emptyJsonMap() {
        return new HashMap<>();
    }

    private boolean externalDbAvailable() {
        try {
            jdbcTemplate.execute("select 1");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @TestConfiguration
    static class ExternalDbDeadlockConfig {
        @Bean
        @Primary
        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
            return org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.JwtDecoder.class);
        }
    }
}
