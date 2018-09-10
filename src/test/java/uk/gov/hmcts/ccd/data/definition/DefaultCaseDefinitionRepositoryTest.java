package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class DefaultCaseDefinitionRepositoryTest {
    private final String JURISDICTION_ID = "Some Jurisdiction";
    private final String JURISDICTION_ID_2 = "Some Jurisdiction 2";
    private final List<String> JURISDICTION_IDS = Lists.newArrayList(JURISDICTION_ID, JURISDICTION_ID_2);
    private final String CTID = "CTID";
    private final CaseTypeDefinitionVersion caseTypeDefinitionVersion = new CaseTypeDefinitionVersion();
    private final List<String> userRoles = Lists.newArrayList("role1", "role2");
    private final Jurisdiction JURISDICTION = new Jurisdiction();
    private final Jurisdiction JURISDICTION_2 = new Jurisdiction();

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    private CaseDefinitionRepository caseDefinitionRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();

        caseDefinitionRepository = new DefaultCaseDefinitionRepository(applicationParams, securityUtils, restTemplate);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldThrowResourceNotFoundExceptionWhenGetCaseTypesForJurisdictionIsCalledAndResourceIsNotFound() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldThrowResourceNotFoundExceptionWhenGetBaseTypesIsCalledAndResourceIsNotFound() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        caseDefinitionRepository.getBaseTypes();
    }

    @Test
    public void shouldReturnLatestVersionWhenGetLatestVersionCalledAndResponseIsReturned() {
        doReturn("http://localhost:1234/latestVersion").when(applicationParams).caseTypeLatestVersionUrl(CTID);
        ResponseEntity<CaseTypeDefinitionVersion> responseEntity = ResponseEntity.ok(caseTypeDefinitionVersion);
        doReturn(responseEntity).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(CTID);
        assertThat(version, is(caseTypeDefinitionVersion));
    }

    @Test
    public void shouldThrowServiceExceptionWhenGetClassificationForUserRoleListCalledAndNullIsReturned() {
        doReturn("http://localhost:1234/classificationForUserRoleList").when(applicationParams).userRolesClassificationsURL();
        doReturn(null).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        ServiceException serviceException = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getClassificationsForUserRoleList(userRoles));
        assertThat(serviceException.getMessage(), is("Error while retrieving classification for user roles [role1, role2] because of: Null response from definition store"));
    }

    @Test
    public void shouldReturnClassificationsForUserRoleListWhenGetClassificationForUserRoleListCalledAndResponseIsReturned() {
        doReturn("http://localhost:1234/print/jurisdictions").when(applicationParams).jurisdictionDefURL();
        ResponseEntity<List<Jurisdiction>> responseEntity = ResponseEntity.ok(Lists.newArrayList(JURISDICTION, JURISDICTION_2));
        doReturn(responseEntity).when(restTemplate).exchange(any(URI.class), any(), any(), any(ParameterizedTypeReference.class));
        List<Jurisdiction> jurisdictions = caseDefinitionRepository.getJurisdictions(JURISDICTION_IDS);
        assertThat(jurisdictions, hasSize(2));
        assertThat(jurisdictions, hasItems(JURISDICTION, JURISDICTION_2));
    }

    @Test
    public void shouldThrowServiceExceptionWhenGetLatestVersionCalledAndNullIsReturned() {
        doReturn("http://localhost:1234/latestVersion").when(applicationParams).caseTypeLatestVersionUrl(CTID);
        doReturn(null).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        ServiceException serviceException = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getLatestVersion(CTID));
        assertThat(serviceException.getMessage(), is("Problem getting case type version for CTID because of: Null response from definition store"));
    }

    @Test
    public void shouldReturnJurisdictionsWhenGetJurisdictionsCalledAndResponseIsReturned() {
        doReturn("http://localhost:1234/print/jurisdictions").when(applicationParams).jurisdictionDefURL();
        ResponseEntity<List<Jurisdiction>> responseEntity = ResponseEntity.ok(Lists.newArrayList(JURISDICTION, JURISDICTION_2));
        doReturn(responseEntity).when(restTemplate).exchange(any(URI.class), any(), any(), any(ParameterizedTypeReference.class));
        List<Jurisdiction> jurisdictions = caseDefinitionRepository.getJurisdictions(JURISDICTION_IDS);
        assertThat(jurisdictions, hasSize(2));
        assertThat(jurisdictions, hasItems(JURISDICTION, JURISDICTION_2));
    }

    @Test
    public void shouldThrowServiceExceptionWhenGetJurisdictionsCalledAndNullIsReturned() {
        doReturn("http://localhost:1234/print/jurisdictions").when(applicationParams).jurisdictionDefURL();
        doReturn(null).when(restTemplate).exchange(any(URI.class), any(), any(), any(Class.class));
        ServiceException serviceException = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getJurisdictions(JURISDICTION_IDS));
        assertThat(serviceException.getMessage(), is("Problem retrieving jurisdictions definition because of: Null response from definition store"));
    }
}
