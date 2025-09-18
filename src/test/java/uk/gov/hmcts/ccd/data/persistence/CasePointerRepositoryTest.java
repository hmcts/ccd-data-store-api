package uk.gov.hmcts.ccd.data.persistence;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

@Transactional
public class CasePointerRepositoryTest extends WireMockBaseTest {

    private static final String JURISDICTION = "TEST_JURISDICTION";
    private static final String CASE_TYPE_DECENTRALIZED = "DecentralizedCaseType";
    private static final Long CASE_REFERENCE = 7777777777777777L;
    private static final String CASE_STATE = "CaseCreated";

    @Inject
    private CasePointerRepository casePointerRepository;

    @Inject
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    private CaseDetails originalCaseDetails;

    @Before
    public void setUp() {
        originalCaseDetails = createOriginalCaseDetails();
    }

    private CaseDetails createOriginalCaseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setCaseTypeId(CASE_TYPE_DECENTRALIZED);
        caseDetails.setState(CASE_STATE);
        caseDetails.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
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
        assertThat(originalCaseDetails.getId(), is(notNullValue()));

        // And: The case pointer should be persisted in the database
        CaseDetails pointer = caseDetailsRepository.findById(Long.valueOf(originalCaseDetails.getId()));
        assertThat("Case pointer should exist in database", pointer, is(notNullValue()));
        assertAll("Case pointer should have expected properties",
            () -> assertThat(pointer.getId(), is(originalCaseDetails.getId())),
            () -> assertThat(pointer.getReference(), is(CASE_REFERENCE)),
            () -> assertThat(pointer.getJurisdiction(), is(JURISDICTION)),
            () -> assertThat(pointer.getCaseTypeId(), is(CASE_TYPE_DECENTRALIZED)),

            // Pointer-specific properties: should be cleared/reset
            () -> assertThat(pointer.getData().isEmpty(), is(true)),
            () -> assertThat(pointer.getState(), is("")),
            () -> assertThat(pointer.getSecurityClassification(), is(SecurityClassification.RESTRICTED)),
            () -> assertThat(pointer.getDataClassification().isEmpty(), is(true)),

            // Database-managed fields: version is set by DB, lastModified is updated on save
            () -> assertThat(pointer.getVersion(), is(notNullValue())),
            () -> assertThat(pointer.getLastModified(), is(notNullValue()))
        );
    }
}
