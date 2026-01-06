package uk.gov.hmcts.ccd.data.persistence;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

@Transactional
public class CasePointerRepositoryTest extends WireMockBaseTest {

    private static final String JURISDICTION = "TEST_JURISDICTION";
    private static final String CASE_TYPE_DECENTRALIZED = "DecentralizedCaseType";
    private static final String CASE_STATE = "CaseCreated";
    private static final AtomicLong CASE_REFERENCE_SEQUENCE = new AtomicLong(7777777777777777L);

    @Inject
    private CasePointerRepository casePointerRepository;

    @Inject
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    private CaseDetails originalCaseDetails;
    private Long currentCaseReference;
    @Inject
    private PlatformTransactionManager transactionManager;

    @Before
    public void setUp() {
        originalCaseDetails = createOriginalCaseDetails();
    }

    private CaseDetails createOriginalCaseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        currentCaseReference = CASE_REFERENCE_SEQUENCE.getAndIncrement();
        caseDetails.setReference(currentCaseReference);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setCaseTypeId(CASE_TYPE_DECENTRALIZED);
        caseDetails.setState(CASE_STATE);
        caseDetails.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setLastStateModifiedDate(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setVersion(1);
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

        caseDetails.setData(Map.of("foo", mapper.valueToTree("bar")));
        caseDetails.setDataClassification(Map.of());

        return caseDetails;
    }

    @Test
    public void persistCasePointer_shouldCreateCasePointerWithEmptyData() {
        // When: Creating a case pointer
        casePointerRepository.persistCasePointerAndInitId(originalCaseDetails);

        // Original case details should not be modified
        assertThat(originalCaseDetails.getData().size(), is(1));
        assertThat(originalCaseDetails.getState(), is(CASE_STATE));
        assertThat(originalCaseDetails.getLastModified(), is(notNullValue()));
        assertThat(originalCaseDetails.getSecurityClassification(), is(SecurityClassification.PUBLIC));
        assertThat(originalCaseDetails.getDataClassification(), is(notNullValue()));
        assertThat(originalCaseDetails.getLastStateModifiedDate(), is(notNullValue()));
        assertThat(originalCaseDetails.getId(), is(notNullValue()));
        assertThat(originalCaseDetails.getResolvedTTL(), is(nullValue()));

        // And: The case pointer should be persisted in the database
        Optional<CaseDetails> pointerOptional = caseDetailsRepository.findById(
            JURISDICTION,
            Long.valueOf(originalCaseDetails.getId())
        );
        assertThat("Case pointer should exist in database", pointerOptional.isPresent(), is(true));
        CaseDetails pointer = pointerOptional.orElseThrow();
        LocalDate expectedDanglingPointerExpiry = LocalDate.now().plusYears(1);
        assertAll("Case pointer should have expected properties",
            () -> assertThat(pointer.getId(), is(originalCaseDetails.getId())),
            () -> assertThat(pointer.getReference(), is(currentCaseReference)),
            () -> assertThat(pointer.getJurisdiction(), is(JURISDICTION)),
            () -> assertThat(pointer.getCaseTypeId(), is(CASE_TYPE_DECENTRALIZED)),

            // Pointer-specific properties: should be cleared/reset
            () -> assertThat(pointer.getData().isEmpty(), is(true)),
            () -> assertThat(pointer.getState(), is("")),
            () -> assertThat(pointer.getSecurityClassification(), is(SecurityClassification.RESTRICTED)),
            () -> assertThat(pointer.getDataClassification().isEmpty(), is(true)),
            () -> assertThat(pointer.getLastStateModifiedDate(), is(nullValue())),
            () -> assertThat(pointer.getResolvedTTL(), is(expectedDanglingPointerExpiry)),

            // Database-managed fields: version is set by DB, lastModified is updated on save
            () -> assertThat(pointer.getVersion(), is(notNullValue())),
            () -> assertThat(pointer.getLastModified(), is(notNullValue()))
        );
    }

    @Test
    public void persistCasePointer_shouldRespectExistingResolvedTtl() {
        LocalDate existingTtl = LocalDate.now().plusMonths(3);
        originalCaseDetails.setResolvedTTL(existingTtl);

        casePointerRepository.persistCasePointerAndInitId(originalCaseDetails);

        CaseDetails pointer = caseDetailsRepository.findById(
            JURISDICTION,
            Long.valueOf(originalCaseDetails.getId())
        ).orElse(null);

        assertThat(pointer, is(notNullValue()));
        assertThat(pointer.getResolvedTTL(), is(existingTtl));
    }

    @Test
    public void persistCasePointer_shouldKeepMarkedByLogstashFlagTrue() {
        casePointerRepository.persistCasePointerAndInitId(originalCaseDetails);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        Boolean markedByLogstash = jdbcTemplate.queryForObject(
            "SELECT marked_by_logstash FROM case_data WHERE reference = ?",
            Boolean.class,
            currentCaseReference
        );

        assertThat(markedByLogstash, is(true));
    }

    @Test
    public void persistRegularCase_shouldUnsetMarkedByLogstashFlag() {
        // Simulate a standard case creation by reusing the original details directly
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        var persisted = caseDetailsRepository.set(originalCaseDetails);

        Boolean markedByLogstash = jdbcTemplate.queryForObject(
            "SELECT marked_by_logstash FROM case_data WHERE id = ?",
            Boolean.class,
            persisted.getId()
        );

        assertThat(markedByLogstash, is(false));
    }

    @Test
    public void constraintShouldPreventUnmarkingCasePointer() {
        casePointerRepository.persistCasePointerAndInitId(originalCaseDetails);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        Long pointerId = Long.valueOf(originalCaseDetails.getId());

        // Run the raw update in its own transaction so an expected constraint failure does not poison
        // the outer test transaction that the tests run in.
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        boolean constraintViolated = false;
        try {
            transactionTemplate.execute(status -> {
                jdbcTemplate.update(
                    "UPDATE case_data SET marked_by_logstash = false WHERE id = ?",
                    pointerId
                );
                return null;
            });
        } catch (RuntimeException expected) {
            constraintViolated = true;
        }

        assertThat("Constraint should prevent unmarking a case pointer", constraintViolated, is(true));
    }
}
