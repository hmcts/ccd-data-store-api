package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/insert_shell_case.sql")
public class SynchronisedCaseProcessorIT extends WireMockBaseTest {

    private static final long CASE_REFERENCE = 1644062237356399L;
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String JURISDICTION = "PROBATE";

    @Autowired
    private SynchronisedCaseProcessor synchronisedCaseProcessor;

    @Inject
    private JdbcTemplate jdbcTemplate;

    private long initialVersion;

    @Before
    public void setUp() {
        initialVersion = getCurrentVersion(CASE_REFERENCE);
    }

    @Test
    public void shouldExecuteOperationAndIncrementVersionWhenPayloadIsNewer() {
        long newVersion = initialVersion + 1L;
        DecentralisedCaseDetails payload = buildCasePayload(newVersion);
        Consumer<CaseDetails> operation = mock(Consumer.class);

        synchronisedCaseProcessor.applyConditionallyWithLock(payload, operation);

        ArgumentCaptor<CaseDetails> captor = ArgumentCaptor.forClass(CaseDetails.class);
        verify(operation).accept(captor.capture());

        CaseDetails processedDetails = captor.getValue();
        assertThat(processedDetails.getReference()).isEqualTo(CASE_REFERENCE);
        assertThat(processedDetails.getCaseTypeId()).isEqualTo(CASE_TYPE_ID);

        assertThat(getCurrentVersion(CASE_REFERENCE)).isEqualTo(newVersion);
    }

    @Test
    public void shouldNotExecuteOperationWhenPayloadVersionIsOlder() {
        long olderVersion = initialVersion - 1L;
        DecentralisedCaseDetails payload = buildCasePayload(olderVersion);
        Consumer<CaseDetails> operation = mock(Consumer.class);

        synchronisedCaseProcessor.applyConditionallyWithLock(payload, operation);

        verify(operation, never()).accept(null);
        assertThat(getCurrentVersion(CASE_REFERENCE)).isEqualTo(initialVersion);
    }

    @Test
    public void shouldNotExecuteOperationWhenPayloadVersionIsTheSame() {
        long sameVersion = initialVersion;
        DecentralisedCaseDetails payload = buildCasePayload(sameVersion);
        Consumer<CaseDetails> operation = mock(Consumer.class);

        synchronisedCaseProcessor.applyConditionallyWithLock(payload, operation);

        verify(operation, never()).accept(null);
        assertThat(getCurrentVersion(CASE_REFERENCE)).isEqualTo(initialVersion);
    }

    private DecentralisedCaseDetails buildCasePayload(Long version) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setId(String.valueOf(CASE_REFERENCE));
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setState("CaseCreated");
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        caseDetails.setCreatedDate(LocalDateTime.now());
        caseDetails.setLastModified(LocalDateTime.now());
        caseDetails.setVersion(version.intValue());
        caseDetails.setData(Collections.singletonMap("PersonFirstName", new TextNode("Test")));

        DecentralisedCaseDetails decentralisedCaseDetails = new DecentralisedCaseDetails();
        decentralisedCaseDetails.setCaseDetails(caseDetails);
        decentralisedCaseDetails.setRevision(version);
        return decentralisedCaseDetails;
    }

    private Long getCurrentVersion(Long caseReference) {
        return jdbcTemplate.queryForObject(
            "SELECT version FROM case_data WHERE reference = ?",
            Long.class,
            caseReference
        );
    }
}
