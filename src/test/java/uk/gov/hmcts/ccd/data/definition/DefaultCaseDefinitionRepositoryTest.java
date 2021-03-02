package uk.gov.hmcts.ccd.data.definition;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
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
    private static final String JURISDICTION_ID = "Some Jurisdiction";

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

    @Test
    public void shouldThrowResourceNotFoundExceptionWhenGetCaseTypesForJurisdictionIsCalled() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () ->
                caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID));
        assertThat(exception.getMessage(), startsWith("Resource not found when getting case types for Jurisdiction"));
    }

    @Test
    public void shouldThrowServiceExceptionWhenGetCaseTypesForJurisdictionIsCalled() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () ->
                caseDefinitionRepository.getCaseTypesForJurisdiction(JURISDICTION_ID));
        assertThat(exception.getMessage(), startsWith("Problem getting case types for the Jurisdiction"));
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

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () -> caseDefinitionRepository.getBaseTypes());
        assertThat(exception.getMessage(), startsWith("Problem getting base types definition from definition store "
            + "because of"));
    }

    @Test
    public void shouldThrowServicedExceptionWhenGetBaseTypesIsCalled() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () -> caseDefinitionRepository.getBaseTypes());
        assertThat(exception.getMessage(), startsWith("Problem getting base types definition from definition store"
            + " because of"));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void shouldReturnFieldTypeListWhenGetBaseTypesIsCalled() {
        FieldTypeDefinition[] fieldTypeDefinitionArr = {new FieldTypeDefinition(), new FieldTypeDefinition()};
        doReturn(fieldTypeDefinitionArr).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        List<FieldTypeDefinition> fieldTypeDefinitions = caseDefinitionRepository.getBaseTypes();
        assertEquals(2, fieldTypeDefinitions.size());
    }

    @Test
    public void shouldFailToGetClassificationForUserRole() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            caseDefinitionRepository.getUserRoleClassifications("nor_defined"));
        assertThat(exception.getMessage(), startsWith("Error while retrieving classification for user role nor_defined"
            + " because of "));
    }


    @Test
    public void shouldFailToGetClassificationForUserRoleThrowsResourceNotFoundException() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () ->
                caseDefinitionRepository.getUserRoleClassifications("nor_defined"));
        assertThat(exception.getMessage(), startsWith("No classification found for user role nor_defined because "
            + "of "));
    }

    @Test
    public void shouldFailToGetLatestVersionFromDefinitionStoreThrowsResourceNotFoundException() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () ->
                caseDefinitionRepository.getLatestVersion("case_type_id"));
        assertThat(exception.getMessage(), startsWith("Resource not found when getting case type version for"));
    }

    @Test
    public void shouldFailToGetLatestVersionFromDefinitionStoreThrowsServiceException() {
        RuntimeException runtimeException = new RuntimeException();
        doThrow(runtimeException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            caseDefinitionRepository.getLatestVersion("case_type_id"));
        assertThat(exception.getMessage(), startsWith("Problem getting case type version for"));
    }

    @Test
    public void shouldGeEmptyClassificationsForEmptyUserRolesList() {
        List<UserRole> emptyUserRoles = caseDefinitionRepository.getClassificationsForUserRoleList(new ArrayList<>());
        assertEquals(0, emptyUserRoles.size());
    }


    @Test
    public void shouldFailToGetJurisdictionThrowsResourceNotFoundException() {
        HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(httpException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class, () ->
                caseDefinitionRepository.getJurisdiction("jurisdiction_id_1"));
        assertThat(exception.getMessage(), startsWith("Resource not found when retrieving jurisdictions definition "
            + "because of"));
    }

    @Test
    public void shouldFailToGetJurisdictionThrowsServiceException() {
        RuntimeException runtimeException = new RuntimeException();
        doThrow(runtimeException).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            caseDefinitionRepository.getJurisdiction("case_type_id"));
        assertThat(exception.getMessage(), startsWith("Problem retrieving jurisdictions definition because of "));
    }

    @Test
    public void shouldGetNullJurisdictionsDefinition() {
        List<JurisdictionDefinition> emptyJurisdictionDefinitions = Lists.newArrayList();
        doReturn(emptyJurisdictionDefinitions).when(restTemplate).exchange(anyString(), any(), any(),
            any(Class.class));

        JurisdictionDefinition jurisdictionDefinition = caseDefinitionRepository.getJurisdiction("PROBATE_NOT_FOUND");
        assertThat(jurisdictionDefinition, nullValue());
    }
}
