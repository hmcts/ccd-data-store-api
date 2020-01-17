/*
package uk.gov.hmcts.ccd.data.definition;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Ignore("for now")
public class DefaultCaseDefinitionRepositoryTest {
    private final String JURISDICTION_ID = "Some Jurisdiction";

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
    public void shouldFailToGetClassificationForUserRole() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () -> caseDefinitionRepository.getUserRoleClassifications("nor_defined"));
        assertThat(exception.getMessage(), startsWith("Error while retrieving classification for user role nor_defined because of "));
    }
}
*/
