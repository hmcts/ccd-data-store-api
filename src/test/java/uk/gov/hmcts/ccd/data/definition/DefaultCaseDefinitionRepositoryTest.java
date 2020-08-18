package uk.gov.hmcts.ccd.data.definition;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DefaultCaseDefinitionRepositoryTest {
    private static final String JURISDICTION_ID = "SomeJurisdiction";
    private static final String CASE_TYPE_ID = "case_type_id";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    private CaseDefinitionRepository caseDefinitionRepository;

    private static final String JURISDICTIONS_URL = "https://domain/api/data/jurisdictions";
    private static final String JURISDICTIONS_CASE_URL = "https://domain/api/data/jurisdictions/case";
    private static final String BASE_TYPES_URL = "/api/base-types";
    private static final String USER_ROLE_URL = "/api/user-role?role={userRole}";
    private static final String CASE_TYPE_URL = "/api/data/case-type/veesion";


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();
        when(applicationParams.jurisdictionDefURL()).thenReturn(JURISDICTIONS_URL);
        when(applicationParams.jurisdictionCaseTypesDefURL(JURISDICTION_ID)).thenReturn(JURISDICTIONS_CASE_URL);
        when(applicationParams.baseTypesURL()).thenReturn(BASE_TYPES_URL);
        when(applicationParams.userRoleClassification()).thenReturn(USER_ROLE_URL);
        when(applicationParams.caseTypeLatestVersionUrl(CASE_TYPE_ID)).thenReturn(CASE_TYPE_URL);

        caseDefinitionRepository = new DefaultCaseDefinitionRepository(applicationParams, securityUtils, restTemplate);
    }

    private void mockGetJurisdictionsFromDefinitionStore(List<JurisdictionDefinition> jurisdictionDefinitions,
                                                         String endpoint) throws URISyntaxException {
        ResponseEntity<List<JurisdictionDefinition>> myEntity =
            new ResponseEntity<List<JurisdictionDefinition>>(jurisdictionDefinitions, HttpStatus.ACCEPTED);

        when(restTemplate.exchange(
            Matchers.eq(new URI(endpoint)),
            Matchers.eq(HttpMethod.GET),
            Matchers.<HttpEntity<List<JurisdictionDefinition>>>any(),
            Matchers.<ParameterizedTypeReference<List<JurisdictionDefinition>>>any())
        ).thenReturn(myEntity);
    }

    private List<JurisdictionDefinition> getJurisdictionDefinition() {

        final List<JurisdictionDefinition> jurisdictionDefinition = new ArrayList<>();
        final JurisdictionDefinition jurisdictionDefinition1 = new JurisdictionDefinition();
        final JurisdictionDefinition jurisdictionDefinition2 = new JurisdictionDefinition();
        jurisdictionDefinition1.setId("jurisdiction1");
        jurisdictionDefinition1.setDescription("kia1");
        jurisdictionDefinition1.setCaseTypeDefinitions(getCaseTypeDefinition("caseId1"));

        jurisdictionDefinition2.setId("jurisdiction2");
        jurisdictionDefinition2.setDescription("kia2");
        jurisdictionDefinition2.setCaseTypeDefinitions(getCaseTypeDefinition("caseId2"));

        jurisdictionDefinition.add(jurisdictionDefinition1);
        jurisdictionDefinition.add(jurisdictionDefinition2);
        return jurisdictionDefinition;
    }

    private List<CaseTypeDefinition> getCaseTypeDefinition(String id1) {
        List<CaseTypeDefinition> caseTypeDefinition = new ArrayList<>();

        CaseTypeDefinition caseTypeDefinition1 = new CaseTypeDefinition();
        caseTypeDefinition1.setId(id1);
        caseTypeDefinition.add(caseTypeDefinition1);
        return caseTypeDefinition;
    }

    @Test
    public void shouldGeEmptyGetCaseTypesIDsByJurisdictions() throws URISyntaxException {
        mockGetJurisdictionsFromDefinitionStore(Collections.emptyList(), JURISDICTIONS_URL + "?ids=jurisdiction1,jurisdiction2");
        final List<String> jurisdictions = newArrayList("jurisdiction1", "jurisdiction2");
        List<String> caseTypes = caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictions);
        assertEquals(0, caseTypes.size());
    }

    @Test
    public void getCaseTypesIDsByJurisdictions() throws URISyntaxException {
        mockGetJurisdictionsFromDefinitionStore(getJurisdictionDefinition(), JURISDICTIONS_URL + "?ids=jurisdiction1,jurisdiction2");
        final List<String> jurisdictions = newArrayList("jurisdiction1", "jurisdiction2");
        List<String> caseTypes = caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictions);
        assertEquals(2, caseTypes.size());
    }

    @Test
    public void getgetAllCaseTypesIDs() throws URISyntaxException {
        mockGetJurisdictionsFromDefinitionStore(getJurisdictionDefinition(), JURISDICTIONS_URL);
        List<String> caseTypes = caseDefinitionRepository.getAllCaseTypesIDs();
        assertEquals(2, caseTypes.size());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldThrowResourceNotFoundExceptionWhenGetCaseTypesForJurisdictionIsCalledAndResourceIsNotFound() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionWhenGetCaseTypesForJurisdictionIsCalled() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () -> caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID));
        assertThat(exception.getMessage(), startsWith("Resource not found when getting case types for Jurisdiction"));
    }

    @Test
    public void shouldThrowServiceExceptionWhenGetCaseTypesForJurisdictionIsCalled() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception =
            assertThrows(ServiceException.class, () -> caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID));
        assertThat(exception.getMessage(), startsWith("Problem getting case types for the Jurisdiction:SomeJurisdiction because of 500 INTERNAL_SERVER_ERROR"));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldThrowResourceNotFoundExceptionWhenGetBaseTypesIsCalledAndResourceIsNotFound() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        caseDefinitionRepository.getBaseTypes();
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionWhenGetBaseTypesIsCalled() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> caseDefinitionRepository.getBaseTypes());
        assertThat(exception.getMessage(), startsWith("Problem getting base types definition from definition store because of"));
    }

    @Test
    public void shouldThrowServicedExceptionWhenGetBaseTypesIsCalled() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getBaseTypes());
        assertThat(exception.getMessage(), startsWith("Problem getting base types definition from definition store because of"));
    }

    public void shouldReturnFieldTypeListWhenGetBaseTypesIsCalled() {
        FieldTypeDefinition[] fieldTypeDefinitionArr = {new FieldTypeDefinition(), new FieldTypeDefinition()};
        ResponseEntity<FieldTypeDefinition[]> myEntity =
            new ResponseEntity<FieldTypeDefinition[]>(fieldTypeDefinitionArr, HttpStatus.ACCEPTED);

        when(restTemplate.exchange(anyString(), any(), any(), any(Class.class))).thenReturn(myEntity);
        List<FieldTypeDefinition> fieldTypeDefinitions = caseDefinitionRepository.getBaseTypes();
        assertEquals(2, fieldTypeDefinitions.size());
    }

    @Test
    public void shouldFailToGetClassificationForUserRole() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getUserRoleClassifications("nor_defined"));
        assertThat(exception.getMessage(), startsWith("Error while retrieving classification for user role nor_defined because of "));
    }

    @Test
    public void shouldFailToGetClassificationForUserRoleThrowsResourceNotFoundException() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), anyMap());
        UserRole userRole = caseDefinitionRepository.getUserRoleClassifications("nor_defined");
        assertNull(userRole);
    }

    @Test
    public void shouldFailToGetLatestVersionFromDefinitionStoreThrowsResourceNotFoundException() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () -> caseDefinitionRepository.getLatestVersion(CASE_TYPE_ID));
        assertThat(exception.getMessage(), startsWith("Error when getting case type version. Unknown case type 'case_type_id'."));
    }

    @Test
    public void shouldFailToGetLatestVersionFromDefinitionStoreThrowsServiceException() {
        RuntimeException runtimeException = new RuntimeException();
        doThrow(runtimeException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getLatestVersion(CASE_TYPE_ID));
        assertThat(exception.getMessage(), startsWith("Problem getting case type version for"));
    }

    @Test
    public void shouldGeEmptyClassificationsForEmptyUserRolesList() {
        List<UserRole> emptyUserRoles = caseDefinitionRepository.getClassificationsForUserRoleList(Collections.emptyList());
        assertEquals(0, emptyUserRoles.size());
    }

    @Test
    public void shouldFailToGetJurisdictionThrowsResourceNotFoundException() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception =
            assertThrows(ServiceException.class, () -> caseDefinitionRepository.getJurisdiction("jurisdiction_id_1"));
        assertThat(exception.getMessage(), startsWith("Problem retrieving jurisdictions definition because of"));
    }

    @Test
    public void shouldFailToGetJurisdictionThrowsServiceException() {
        RuntimeException runtimeException = new RuntimeException();
        doThrow(runtimeException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getJurisdiction(CASE_TYPE_ID));
        assertThat(exception.getMessage(), startsWith("Problem retrieving jurisdictions definition because of "));
    }

    @Test
    public void shouldGetNullJurisdictionsDefinition() throws URISyntaxException {
        mockGetJurisdictionsFromDefinitionStore(Collections.emptyList(), JURISDICTIONS_URL + "?ids=PROBATE_NOT_FOUND");
        JurisdictionDefinition jurisdictionDefinition = caseDefinitionRepository.getJurisdiction("PROBATE_NOT_FOUND");
        assertThat(jurisdictionDefinition, nullValue());
    }
}
