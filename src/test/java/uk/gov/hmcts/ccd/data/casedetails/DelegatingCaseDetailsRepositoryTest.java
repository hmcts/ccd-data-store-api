package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.ccd.decentralised.client.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DelegatingCaseDetailsRepositoryTest {

    private static final String JURISDICTION = "TEST_JURISDICTION";
    private static final String CASE_TYPE_CENTRALIZED = "CentralizedType";
    private static final String CASE_TYPE_DECENTRALIZED = "DecentralizedType";
    private static final Long CASE_ID = 12345L;
    private static final Long CASE_REFERENCE = 1234567890L;
    private static final String CASE_REFERENCE_STRING = "1234567890";

    @Mock
    private PersistenceStrategyResolver resolver;

    @Mock
    private ServicePersistenceClient decentralisedClient;

    @Mock
    private DefaultCaseDetailsRepository localRepository;

    @InjectMocks
    private DelegatingCaseDetailsRepository repository;

    private CaseDetails centralizedCaseDetails;
    private CaseDetails decentralizedCaseDetails;
    private CaseDetails decentralizedFullCaseDetails;

    @Before
    public void setUp() {
        centralizedCaseDetails = createCaseDetails(CASE_TYPE_CENTRALIZED);
        decentralizedCaseDetails = createCaseDetails(CASE_TYPE_DECENTRALIZED);
        decentralizedFullCaseDetails = createCaseDetails(CASE_TYPE_DECENTRALIZED);
        decentralizedFullCaseDetails.setId("full-case-id");
    }

    private CaseDetails createCaseDetails(String caseType) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(CASE_ID));
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setCaseTypeId(caseType);
        caseDetails.setState("TestState");
        caseDetails.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        return caseDetails;
    }

    @Test
    public void set_shouldThrowUnsupportedOperationException_whenCaseIsDecentralized() {
        when(resolver.isDecentralised(decentralizedCaseDetails)).thenReturn(true);

        assertThrows(UnsupportedOperationException.class, () -> repository.set(decentralizedCaseDetails));

        verify(localRepository, never()).set(any());
    }

    @Test
    public void set_shouldDelegateToLocalRepository_whenCaseIsCentralized() {
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);
        when(localRepository.set(centralizedCaseDetails)).thenReturn(centralizedCaseDetails);

        CaseDetails result = repository.set(centralizedCaseDetails);

        assertThat(result, is(sameInstance(centralizedCaseDetails)));
        verify(localRepository).set(centralizedCaseDetails);
    }

    @Test
    public void findByReferenceWithNoAccessControl_shouldReturnEmpty_whenLocalRepositoryReturnsEmpty() {
        when(localRepository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STRING))
            .thenReturn(Optional.empty());

        Optional<CaseDetails> result = repository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STRING);

        assertThat(result.isPresent(), is(false));
        verify(decentralisedClient, never()).getCase(any());
    }

    @Test
    public void findByReferenceWithNoAccessControl_shouldReturnLocalCase_whenCaseIsCentralized() {
        when(localRepository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STRING))
            .thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        Optional<CaseDetails> result = repository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STRING);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(sameInstance(centralizedCaseDetails)));
        verify(decentralisedClient, never()).getCase(any());
    }

    @Test
    public void findByReferenceWithNoAccessControl_shouldReturnDecentralizedCase_whenCaseIsDecentralized() {
        when(localRepository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STRING))
            .thenReturn(Optional.of(decentralizedCaseDetails));
        when(resolver.isDecentralised(decentralizedCaseDetails)).thenReturn(true);
        when(decentralisedClient.getCase(decentralizedCaseDetails)).thenReturn(decentralizedFullCaseDetails);

        Optional<CaseDetails> result = repository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STRING);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(sameInstance(decentralizedFullCaseDetails)));
        verify(decentralisedClient).getCase(decentralizedCaseDetails);
    }

    @Test
    public void findById_withJurisdiction_shouldReturnEmpty_whenLocalRepositoryReturnsEmpty() {
        when(localRepository.findById(JURISDICTION, CASE_ID)).thenReturn(Optional.empty());

        Optional<CaseDetails> result = repository.findById(JURISDICTION, CASE_ID);

        assertThat(result.isPresent(), is(false));
        verify(decentralisedClient, never()).getCase(any());
    }

    @Test
    public void findById_withJurisdiction_shouldReturnLocalCase_whenCaseIsCentralized() {
        when(localRepository.findById(JURISDICTION, CASE_ID)).thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        Optional<CaseDetails> result = repository.findById(JURISDICTION, CASE_ID);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(sameInstance(centralizedCaseDetails)));
        verify(decentralisedClient, never()).getCase(any());
    }

    @Test
    public void findById_withJurisdiction_shouldReturnDecentralizedCase_whenCaseIsDecentralized() {
        when(localRepository.findById(JURISDICTION, CASE_ID)).thenReturn(Optional.of(decentralizedCaseDetails));
        when(resolver.isDecentralised(decentralizedCaseDetails)).thenReturn(true);
        when(decentralisedClient.getCase(decentralizedCaseDetails)).thenReturn(decentralizedFullCaseDetails);

        Optional<CaseDetails> result = repository.findById(JURISDICTION, CASE_ID);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(sameInstance(decentralizedFullCaseDetails)));
        verify(decentralisedClient).getCase(decentralizedCaseDetails);
    }

    @Test
    public void findById_deprecated_shouldReturnNull_whenCaseNotFound() {
        when(localRepository.findById(null, CASE_ID)).thenReturn(Optional.empty());

        CaseDetails result = repository.findById(CASE_ID);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void findById_deprecated_shouldReturnCase_whenCaseFound() {
        when(localRepository.findById(null, CASE_ID)).thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        CaseDetails result = repository.findById(CASE_ID);

        assertThat(result, is(sameInstance(centralizedCaseDetails)));
    }

    @Test
    public void findByReference_withJurisdictionAndLongReference_shouldDelegateCorrectly() {
        when(localRepository.findByReference(JURISDICTION, CASE_REFERENCE))
            .thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        Optional<CaseDetails> result = repository.findByReference(JURISDICTION, CASE_REFERENCE);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(sameInstance(centralizedCaseDetails)));
        verify(localRepository).findByReference(JURISDICTION, CASE_REFERENCE);
    }

    @Test
    public void findByReference_withJurisdictionAndStringReference_shouldParseAndDelegate() {
        when(localRepository.findByReference(JURISDICTION, CASE_REFERENCE))
            .thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        Optional<CaseDetails> result = repository.findByReference(JURISDICTION, CASE_REFERENCE_STRING);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(sameInstance(centralizedCaseDetails)));
        verify(localRepository).findByReference(JURISDICTION, CASE_REFERENCE);
    }

    @Test
    public void findByReference_withStringReference_shouldParseAndDelegateWithNullJurisdiction() {
        when(localRepository.findByReference(null, CASE_REFERENCE))
            .thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        Optional<CaseDetails> result = repository.findByReference(CASE_REFERENCE_STRING);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(sameInstance(centralizedCaseDetails)));
        verify(localRepository).findByReference(null, CASE_REFERENCE);
    }

    @Test
    public void findByReference_deprecated_shouldThrowResourceNotFoundException_whenCaseNotFound() {
        when(localRepository.findByReference(null, CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repository.findByReference(CASE_REFERENCE));
    }

    @Test
    public void findByReference_deprecated_shouldReturnCase_whenCaseFound() {
        when(localRepository.findByReference(null, CASE_REFERENCE))
            .thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        CaseDetails result = repository.findByReference(CASE_REFERENCE);

        assertThat(result, is(sameInstance(centralizedCaseDetails)));
    }

    @Test
    public void findUniqueCase_deprecated_shouldReturnNull_whenCaseNotFound() {
        when(localRepository.findByReference(JURISDICTION, CASE_REFERENCE)).thenReturn(Optional.empty());

        CaseDetails result = repository.findUniqueCase(JURISDICTION, CASE_TYPE_CENTRALIZED, CASE_REFERENCE_STRING);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void findUniqueCase_deprecated_shouldReturnCase_whenCaseFound() {
        when(localRepository.findByReference(JURISDICTION, CASE_REFERENCE))
            .thenReturn(Optional.of(centralizedCaseDetails));
        when(resolver.isDecentralised(centralizedCaseDetails)).thenReturn(false);

        CaseDetails result = repository.findUniqueCase(JURISDICTION, CASE_TYPE_CENTRALIZED, CASE_REFERENCE_STRING);

        assertThat(result, is(sameInstance(centralizedCaseDetails)));
    }

    @Test
    public void findCaseReferencesByIds_shouldAlwaysDelegateToLocalRepository() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<Long> expectedReferences = Arrays.asList(100L, 200L, 300L);
        when(localRepository.findCaseReferencesByIds(ids)).thenReturn(expectedReferences);

        List<Long> result = repository.findCaseReferencesByIds(ids);

        assertThat(result, is(equalTo(expectedReferences)));
        verify(localRepository).findCaseReferencesByIds(ids);
    }
}
