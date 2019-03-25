package uk.gov.hmcts.ccd.data.definition;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

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

    private CaseDefinitionRepository caseDefinitionRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();
        doReturn("jurisdictionCaseTypes").when(applicationParams).jurisdictionCaseTypesDefURL(JURISDICTION_ID);
        doReturn("baseTypes").when(applicationParams).baseTypesURL();
        doReturn("caseType").when(applicationParams).caseTypeDefURL(CASE_TYPE_ID);
        doReturn("caseTypeReferences").when(applicationParams).caseTypesReferencesDefURL();

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
    public void shouldThrowServiceExceptionWithCorrectMsgWhenGetUserRoleClassificationsAndServerError() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getUserRoleClassifications("nor_defined"));
        assertThat(exception.getMessage(), startsWith("Error while retrieving classification for user role nor_defined because of "));
    }
}
