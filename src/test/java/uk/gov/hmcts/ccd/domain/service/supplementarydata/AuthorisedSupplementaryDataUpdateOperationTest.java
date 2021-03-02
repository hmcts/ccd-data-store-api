package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.EndpointAuthorisationService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorisedSupplementaryDataUpdateOperationTest {

    private static final String CASE_REFERENCE = "12345677";

    private AuthorisedSupplementaryDataUpdateOperation supplementaryDataOperation;

    @Mock
    private DefaultSupplementaryDataUpdateOperation defaultSupplementaryDataOperation;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private EndpointAuthorisationService endpointAuthorisationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        supplementaryDataOperation = new AuthorisedSupplementaryDataUpdateOperation(defaultSupplementaryDataOperation,
            caseDetailsRepository,
            endpointAuthorisationService);
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetailsRepository.findByReference(anyString())).thenReturn(Optional.of(caseDetails));
    }

    @Test
    void shouldThrowExceptionWhenCaseNotFound() {
        when(caseDetailsRepository.findByReference(anyString())).thenReturn(Optional.empty());
        assertThrows(
            CaseNotFoundException.class, () -> supplementaryDataOperation
                .updateSupplementaryData(CASE_REFERENCE, new SupplementaryDataUpdateRequest())
        );
    }

    @Test
    void shouldThrowRoleValidationExceptionWhenUserNotAuthorised() {
        when(endpointAuthorisationService.isAccessAllowed(any())).thenReturn(false);
        assertThrows(
            CaseAccessException.class, () -> supplementaryDataOperation
                .updateSupplementaryData(CASE_REFERENCE, new SupplementaryDataUpdateRequest())
        );
    }

    @Test
    void shouldInvokeRepositoryWhenCaseFoundAndUserAuthorised() {
        when(endpointAuthorisationService.isAccessAllowed(any())).thenReturn(true);
        SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();
        SupplementaryData responseExpected = new SupplementaryData(new HashMap<>());
        when(defaultSupplementaryDataOperation.updateSupplementaryData(CASE_REFERENCE, request))
                .thenReturn(responseExpected);
        SupplementaryData supplementaryData = supplementaryDataOperation.updateSupplementaryData(CASE_REFERENCE,
                request);
        assertNotNull(supplementaryData);
        assertEquals(responseExpected, supplementaryData);
        assertEquals(responseExpected.getResponse().size(), supplementaryData.getResponse().size());
    }

}
