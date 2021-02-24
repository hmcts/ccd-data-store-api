package uk.gov.hmcts.ccd.domain.service.getcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class AccessControlledGetCaseOperationTest {
    public static final String CASE_REFERENCE = "caseReference";
    @Mock
    private GetCaseOperation getCaseOperation;
    @Mock
    private GetCaseOperation creatorGetCaseOperation;
    @Mock
    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private CaseDetails caseDetails;
    @Mock
    private CaseDetails accessControlledCaseDetails;

    private AccessControlledGetCaseOperation instance;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        instance = new AccessControlledGetCaseOperation(creatorGetCaseOperation,
                                                        getCaseOperation,
                                                        defaultCaseDataAccessControl,
                                                        applicationParams);
    }

    @Test
    public void shouldGetCaseWithAppliedAccessControl() {
        given(applicationParams.getEnableAttributeBasedAccessControl()).willReturn(true);
        given(getCaseOperation.execute(CASE_REFERENCE)).willReturn(Optional.of(caseDetails));
        given(defaultCaseDataAccessControl.applyAccessControl(caseDetails))
            .willReturn(Optional.of(accessControlledCaseDetails));

        Optional<CaseDetails> result = instance.execute(CASE_REFERENCE);

        assertTrue(result.isPresent());
        assertEquals(accessControlledCaseDetails, result.get());
    }

    @Test
    public void shouldGetCaseWithAppliedAccessControlReturnsEmptyCaseDetails() {
        given(applicationParams.getEnableAttributeBasedAccessControl()).willReturn(true);
        given(getCaseOperation.execute(CASE_REFERENCE)).willReturn(Optional.of(caseDetails));
        given(defaultCaseDataAccessControl.applyAccessControl(caseDetails)).willReturn(Optional.empty());

        Optional<CaseDetails> result = instance.execute(CASE_REFERENCE);

        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldDelegateToCreatorGetCaseOperationAndIgnoreJurisdictionAndCaseType() {
        given(applicationParams.getEnableAttributeBasedAccessControl()).willReturn(false);

        instance.execute("j", "ct", CASE_REFERENCE);

        verify(creatorGetCaseOperation).execute(CASE_REFERENCE);
    }

    @Test
    public void shouldDelegateToCreatorGetCaseOperation() {
        given(applicationParams.getEnableAttributeBasedAccessControl()).willReturn(false);

        instance.execute(CASE_REFERENCE);

        verify(creatorGetCaseOperation).execute(CASE_REFERENCE);
    }
}
