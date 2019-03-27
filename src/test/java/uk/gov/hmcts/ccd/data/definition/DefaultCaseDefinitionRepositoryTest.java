package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import org.hamcrest.collection.IsEmptyCollection;
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
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class DefaultCaseDefinitionRepositoryTest {
    private final String JURISDICTION_ID = "Some Jurisdiction";
    private final String CASE_TYPE_ID = "Some Case Type";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity responseEntity;

    private CaseDefinitionRepository caseDefinitionRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();
        doReturn("userRoleClassification").when(applicationParams).userRoleClassification();
        doReturn("userRolesClassifications").when(applicationParams).userRolesClassificationsURL();
        doReturn("caseTypeLatestVersion").when(applicationParams).caseTypeLatestVersionUrl(CASE_TYPE_ID);
        doReturn("jurisdictionCaseTypes").when(applicationParams).jurisdictionCaseTypesDefURL(JURISDICTION_ID);
        doReturn("baseTypes").when(applicationParams).baseTypesURL();
        doReturn("caseType").when(applicationParams).caseTypeDefURL(CASE_TYPE_ID);
        doReturn("caseTypeReferences").when(applicationParams).caseTypesReferencesDefURL();
        doReturn("http://jurisdictions").when(applicationParams).jurisdictionDefURL();

        caseDefinitionRepository = new DefaultCaseDefinitionRepository(applicationParams, securityUtils, restTemplate);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldThrowResourceNotFoundExceptionWhenGetCaseTypesForJurisdictionAndResourceIsNotFound() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID);
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenCaseTypesForJurisdictionAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID));
        assertThat(exception.getMessage(), startsWith(String.format("Problem getting case types for the Jurisdiction:%s", JURISDICTION_ID)));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldThrowResourceNotFoundExceptionWhenGetBaseTypesAndResourceIsNotFound() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        caseDefinitionRepository.getBaseTypes();
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetBaseTypesAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getBaseTypes());
        assertThat(exception.getMessage(), startsWith("Problem getting base types definition from definition store because of "));
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionWhenGetCaseTypeAndResourceIsNotFound() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> caseDefinitionRepository.getCaseType(CASE_TYPE_ID));
        assertThat(exception.getMessage(), startsWith(String.format("Resource not found when getting case type definition for %s because of ", CASE_TYPE_ID)));
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetCaseTypeAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getCaseType(CASE_TYPE_ID));
        assertThat(exception.getMessage(), startsWith(String.format("Problem getting case type definition for %s because of ", CASE_TYPE_ID)));
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionWithCorrectMessageWhenGetCaseTypeReferencesAndResourcesNotFound() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> caseDefinitionRepository.getCaseTypesReferences());
        assertThat(exception.getMessage(), startsWith("Resource not found when getting case type references because of "));
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetCaseTypeReferencesAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getCaseTypesReferences());
        assertThat(exception.getMessage(), startsWith("Problem getting case type references because of "));
    }

    @Test
    public void shouldReturnNullWhenGetUserRoleClassificationsAndResourcesNotFound() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), any(Map.class));

        UserRole userRole = caseDefinitionRepository.getUserRoleClassifications("nor_defined");
        assertThat(userRole, is(nullValue()));
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetUserRoleClassificationsAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), any(Map.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getUserRoleClassifications("nor_defined"));
        assertThat(exception.getMessage(), startsWith("Error while retrieving classification for user role nor_defined because of "));
    }

    @Test
    public void shouldReturnEmptyWhenGetClassificationsForUserRoleListWithEmptyUserRoles() {
        doReturn(responseEntity).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), any(Map.class));

        List<UserRole> classificationsForUserRoleList = caseDefinitionRepository.getClassificationsForUserRoleList(Lists.newArrayList());
        assertThat(classificationsForUserRoleList, IsEmptyCollection.empty());
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionWithCorrectMessageWhenGetClassificationsForUserRoleListAndResourcesNotFound() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), any(Map.class));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> caseDefinitionRepository.getClassificationsForUserRoleList(Lists.newArrayList("nor_defined", "nor_defined2")));
        assertThat(exception.getMessage(), startsWith("No classification for user roles [nor_defined, nor_defined2] because of "));
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetClassificationsForUserRoleListAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), any(Map.class));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> caseDefinitionRepository.getClassificationsForUserRoleList(Lists.newArrayList("nor_defined", "nor_defined2")));
        assertThat(exception.getMessage(), startsWith("Error while retrieving classifications for user roles [nor_defined, nor_defined2] because of "));
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionWithCorrectMessageWhenGetLatestVersionAndResourcesNotFound() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> caseDefinitionRepository.getLatestVersion(CASE_TYPE_ID));
        assertThat(exception.getMessage(), startsWith("Resource not found when getting case type version for " + CASE_TYPE_ID + " because of "));
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetLatestVersionAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> caseDefinitionRepository.getLatestVersion(CASE_TYPE_ID));
        assertThat(exception.getMessage(), startsWith("Problem getting case type version for " + CASE_TYPE_ID + " because of "));
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionWithCorrectMessageWhenGetJurisdictionsAndResourcesNotFound() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(any(URI.class), any(), any(), any(ParameterizedTypeReference.class));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> caseDefinitionRepository.getJurisdictions(Lists.newArrayList(JURISDICTION_ID, "another jurisdiction")));
        assertThat(exception.getMessage(), startsWith("Resource not found when retrieving jurisdictions definition because of "));
    }

    @Test
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetJurisdictionsAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(any(URI.class), any(), any(), any(ParameterizedTypeReference.class));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> caseDefinitionRepository.getJurisdictions(Lists.newArrayList(JURISDICTION_ID, "another jurisdiction")));
        assertThat(exception.getMessage(), startsWith("Problem retrieving jurisdictions definition because of "));
    }
}
