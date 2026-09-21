package uk.gov.hmcts.ccd.data.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Transactional
class CasePointerRepositoryTest extends WireMockBaseTest {

    private static final String JURISDICTION = "TEST_JURISDICTION";
    private static final String CASE_TYPE_DECENTRALIZED = "DecentralizedCaseType";
    private static final String CASE_STATE = "CaseCreated";
    private static final Path COALESCING_MIGRATION = Path.of(
        "src/main/resources/db/migration/V20260918_0000__CCD-4262_coalesce_logstash_queue_rows.sql");

    private static String logstashPollStatement(int batchSize) {
        return "WITH candidates AS (SELECT q.id FROM case_data_logstash_queue q ORDER BY q.id "
            + "FOR UPDATE SKIP LOCKED LIMIT " + batchSize + ") "
            + "DELETE FROM case_data_logstash_queue q USING candidates c, case_data "
            + "WHERE q.id = c.id AND q.case_data_id = case_data.id "
            + "RETURNING q.id AS version, case_data.id, created_date, last_modified, "
            + "jurisdiction, case_type_id, state, "
            + "last_state_modified_date, data::TEXT as json_data, data_classification::TEXT "
            + "as json_data_classification, reference, security_classification, supplementary_data::TEXT "
            + "as json_supplementary_data";
    }

    private static final AtomicLong CASE_REFERENCE_SEQUENCE = new AtomicLong(7777777777777777L);

    @Inject
    private CasePointerRepository casePointerRepository;

    @Inject
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private SupplementaryDataRepository supplementaryDataRepository;

    @PersistenceContext
    private EntityManager em;

    private CaseDetails originalCaseDetails;
    private Long currentCaseReference;

    @BeforeEach
    void setUp() {
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
    void persistCasePointer_shouldCreateCasePointerWithEmptyData() {
        // When: Creating a case pointer
        casePointerRepository.persistCasePointerAndInitId(originalCaseDetails);

        // Original case details should not be modified
        assertThat(originalCaseDetails.getData()).hasSize(1);
        assertThat(originalCaseDetails.getState()).isEqualTo(CASE_STATE);
        assertThat(originalCaseDetails.getLastModified()).isNotNull();
        assertThat(originalCaseDetails.getSecurityClassification()).isEqualTo(SecurityClassification.PUBLIC);
        assertThat(originalCaseDetails.getDataClassification()).isNotNull();
        assertThat(originalCaseDetails.getLastStateModifiedDate()).isNotNull();
        assertThat(originalCaseDetails.getId()).isNotNull();
        assertThat(originalCaseDetails.getResolvedTTL()).isNull();

        // And: The case pointer should be persisted in the database
        Optional<CaseDetails> pointerOptional = caseDetailsRepository.findById(
            JURISDICTION,
            Long.valueOf(originalCaseDetails.getId())
        );
        assertThat(pointerOptional).as("Case pointer should exist in database").isPresent();
        CaseDetails pointer = pointerOptional.orElseThrow();
        LocalDate expectedDanglingPointerExpiry = LocalDate.now().plusYears(1);
        assertAll("Case pointer should have expected properties",
            () -> assertThat(pointer.getId()).isEqualTo(originalCaseDetails.getId()),
            () -> assertThat(pointer.getReference()).isEqualTo(currentCaseReference),
            () -> assertThat(pointer.getJurisdiction()).isEqualTo(JURISDICTION),
            () -> assertThat(pointer.getCaseTypeId()).isEqualTo(CASE_TYPE_DECENTRALIZED),

            // Pointer-specific properties: should be cleared/reset
            () -> assertThat(pointer.getData()).isEmpty(),
            () -> assertThat(pointer.getState()).isEmpty(),
            () -> assertThat(pointer.getSecurityClassification()).isEqualTo(SecurityClassification.RESTRICTED),
            () -> assertThat(pointer.getDataClassification()).isEmpty(),
            () -> assertThat(pointer.getLastStateModifiedDate()).isNull(),
            () -> assertThat(pointer.getResolvedTTL()).isEqualTo(expectedDanglingPointerExpiry),

            // Database-managed fields: version is set by DB, lastModified is updated on save
            () -> assertThat(pointer.getVersion()).isNotNull(),
            () -> assertThat(pointer.getLastModified()).isNotNull()
        );
    }

    @Test
    void persistCasePointer_shouldRespectExistingResolvedTtl() {
        LocalDate existingTtl = LocalDate.now().plusMonths(3);
        originalCaseDetails.setResolvedTTL(existingTtl);

        casePointerRepository.persistCasePointerAndInitId(originalCaseDetails);

        CaseDetails pointer = caseDetailsRepository.findById(
            JURISDICTION,
            Long.valueOf(originalCaseDetails.getId())
        ).orElse(null);

        assertThat(pointer).isNotNull();
        assertThat(pointer.getResolvedTTL()).isEqualTo(existingTtl);
    }

    @Test
    void persistCasePointer_shouldNotQueueCasePointerForLogstash() {
        casePointerRepository.persistCasePointerAndInitId(originalCaseDetails);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        Integer queueEntries = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM case_data_logstash_queue WHERE case_data_id = ?",
            Integer.class,
            Long.valueOf(originalCaseDetails.getId())
        );

        assertThat(queueEntries).isZero();
    }

    @Test
    void persistRegularCase_shouldQueueForLogstash() {
        // Simulate a standard case creation by reusing the original details directly
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        var persisted = caseDetailsRepository.set(originalCaseDetails);

        Integer queueEntries = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM case_data_logstash_queue WHERE case_data_id = ?",
            Integer.class,
            persisted.getId()
        );

        assertThat(queueEntries).isOne();
    }

    @Test
    void logstashQueueShouldCoalesceWritesAndReturnTheLatestCaseState() {
        // One pending queue row represents the latest state of a frequently updated case.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        CaseDetails persisted = caseDetailsRepository.set(originalCaseDetails);

        persisted.setData(Map.of("foo", mapper.valueToTree("baz")));
        CaseDetails updated = caseDetailsRepository.set(persisted);

        assertThat(countQueuedRows(jdbcTemplate, updated.getId())).isOne();
        Long queuedId = jdbcTemplate.queryForObject(
            "SELECT id FROM case_data_logstash_queue WHERE case_data_id = ?",
            Long.class, Long.valueOf(updated.getId()));

        List<Map<String, Object>> polledRows = jdbcTemplate.queryForList(logstashPollStatement(1000));

        assertAll(
            () -> assertThat(polledRows).hasSize(1),
            () -> assertThat(polledRows.stream().allMatch(row -> row.get("id").toString().equals(updated.getId())))
                .isTrue(),
            () -> assertThat(polledRows).extracting(row -> ((Number) row.get("version")).longValue())
                .containsExactly(queuedId),
            () -> assertThat(polledRows.stream().allMatch(row -> row.get("json_data").toString().contains("baz")))
                .isTrue(),
            () -> assertThat(countQueuedRows(jdbcTemplate, updated.getId())).isZero()
        );
    }

    @Test
    void logstashPollingShouldLeaveRowsBeyondItsBatchForTheNextPoll() {
        caseDetailsRepository.set(originalCaseDetails);
        caseDetailsRepository.set(createOriginalCaseDetails());
        caseDetailsRepository.set(createOriginalCaseDetails());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);

        List<Map<String, Object>> firstBatch = jdbcTemplate.queryForList(logstashPollStatement(2));

        assertAll(
            () -> assertThat(firstBatch).hasSize(2),
            () -> assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM case_data_logstash_queue", Integer.class)).isOne(),
            () -> assertThat(jdbcTemplate.queryForList(logstashPollStatement(2))).hasSize(1),
            () -> assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM case_data_logstash_queue", Integer.class)).isZero()
        );
    }

    @Test
    void coalescingMigrationShouldKeepOneFreshRegularCaseRowAndDiscardLegacyPointerRows() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        final CaseDetails regularCase = caseDetailsRepository.set(originalCaseDetails);
        final CaseDetails pointerSource = createOriginalCaseDetails();
        casePointerRepository.persistCasePointerAndInitId(pointerSource);

        jdbcTemplate.update("DELETE FROM case_data_logstash_queue");
        jdbcTemplate.execute("ALTER TABLE case_data_logstash_queue "
            + "DROP CONSTRAINT case_data_logstash_queue_case_data_id_unique");
        jdbcTemplate.update("INSERT INTO case_data_logstash_queue (case_data_id) VALUES (?)", regularCase.getId());
        jdbcTemplate.update("INSERT INTO case_data_logstash_queue (case_data_id) VALUES (?)", regularCase.getId());
        jdbcTemplate.update("INSERT INTO case_data_logstash_queue (case_data_id) VALUES (?)", pointerSource.getId());
        Long previousHighestQueueId = jdbcTemplate.queryForObject(
            "SELECT max(id) FROM case_data_logstash_queue", Long.class);

        jdbcTemplate.execute(Files.readString(COALESCING_MIGRATION));

        Long freshQueueId = jdbcTemplate.queryForObject(
            "SELECT id FROM case_data_logstash_queue WHERE case_data_id = ?",
            Long.class, regularCase.getId());

        assertAll(
            () -> assertThat(countQueuedRows(jdbcTemplate, regularCase.getId())).isOne(),
            () -> assertThat(countQueuedRows(jdbcTemplate, pointerSource.getId())).isZero(),
            () -> assertThat(freshQueueId).isGreaterThan(previousHighestQueueId),
            () -> assertThat(jdbcTemplate.update(
                "INSERT INTO case_data_logstash_queue (case_data_id) VALUES (?) "
                    + "ON CONFLICT (case_data_id) DO NOTHING", regularCase.getId())).isZero()
        );
    }

    @Test
    void supplementaryDataOnlyUpdateShouldUseItsQueueIdAsExternalVersion() {
        // Supplementary-data-only writes intentionally do not change case_data.version. The queue id is monotonic,
        // so it must be used as the external Elasticsearch version.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        CaseDetails persisted = caseDetailsRepository.set(originalCaseDetails);
        final int versionBeforeSupplementaryDataWrite = persisted.getVersion();

        // Drain the row queued by the create so only the supplementary-data write is under test.
        jdbcTemplate.update("DELETE FROM case_data_logstash_queue");

        supplementaryDataRepository.setSupplementaryData(
            persisted.getReferenceAsString(), "orgs_assigned_users.OrgA", 3);
        em.flush();

        Integer versionAfterSupplementaryDataWrite = jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE id = ?", Integer.class, Long.valueOf(persisted.getId()));
        Long queuedExternalVersion = jdbcTemplate.queryForObject(
            "SELECT id FROM case_data_logstash_queue WHERE case_data_id = ?",
            Long.class,
            Long.valueOf(persisted.getId())
        );
        List<Map<String, Object>> polledRows = jdbcTemplate.queryForList(logstashPollStatement(1000));

        assertAll(
            () -> assertThat(countQueuedRows(jdbcTemplate, persisted.getId()))
                .as("the trigger fires on supplementary_data, so the case is queued for indexing")
                .isZero(),
            () -> assertThat(polledRows)
                .as("the supplementary-data write really was picked up by the Logstash poll")
                .hasSize(1),
            () -> assertThat(polledRows.get(0).get("json_supplementary_data").toString())
                .as("the polled row carries the new supplementary data")
                .contains("OrgA"),
            () -> assertThat(versionAfterSupplementaryDataWrite)
                .as("case_data.version is NOT advanced by a supplementary-data-only write")
                .isEqualTo(versionBeforeSupplementaryDataWrite),
            () -> assertThat(((Number) polledRows.get(0).get("version")).intValue())
                .as("the external version is the monotonic queue row id")
                .isEqualTo(queuedExternalVersion.intValue())
        );
    }

    @Test
    void laterSupplementaryDataUpdateShouldHaveHigherQueueVersionThanAnAlreadyPolledUpdate() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        CaseDetails persisted = caseDetailsRepository.set(originalCaseDetails);
        jdbcTemplate.update("DELETE FROM case_data_logstash_queue");

        supplementaryDataRepository.setSupplementaryData(
            persisted.getReferenceAsString(), "orgs_assigned_users.OrgA", 1);
        em.flush();
        Map<String, Object> firstPolledRow = onlyPolledRow(jdbcTemplate);

        supplementaryDataRepository.setSupplementaryData(
            persisted.getReferenceAsString(), "orgs_assigned_users.OrgA", 2);
        em.flush();
        Map<String, Object> secondPolledRow = onlyPolledRow(jdbcTemplate);

        assertAll(
            () -> assertThat(((Number) secondPolledRow.get("version")).longValue())
                .isGreaterThan(((Number) firstPolledRow.get("version")).longValue()),
            () -> assertThat(mapper.readTree(firstPolledRow.get("json_supplementary_data").toString())
                .at("/orgs_assigned_users/OrgA").asInt()).isEqualTo(1),
            () -> assertThat(mapper.readTree(secondPolledRow.get("json_supplementary_data").toString())
                .at("/orgs_assigned_users/OrgA").asInt()).isEqualTo(2)
        );
    }

    private Map<String, Object> onlyPolledRow(JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(logstashPollStatement(1000));
        assertThat(rows).as("one queued case should be returned by the Logstash poll").hasSize(1);
        return rows.get(0);
    }

    private Integer countQueuedRows(JdbcTemplate jdbcTemplate, String caseDataId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM case_data_logstash_queue WHERE case_data_id = ?",
            Integer.class,
            Long.valueOf(caseDataId)
        );
    }
}
