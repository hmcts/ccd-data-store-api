package uk.gov.hmcts.ccd.cache;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.idam.AuthenticatedUser;
import uk.gov.hmcts.ccd.idam.IdamHelper;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.List;

import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultCacheWarmUpServiceTest {

    @Mock
    private DefaultCaseDefinitionRepository defaultCaseDefinitionRepository;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private IdamHelper idamHelper;
    @Mock
    private AuthenticatedUser authenticatedUser;

    private String serviceToken = "serviceToken";
    private String userToken = "accessToken";
    private HttpHeaders headers;
    private List<String> caseTypeReferences = Lists.newArrayList("CT1", "CT2", "CT3");

    private DefaultCacheWarmUpService defaultCacheWarmUpService;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(authenticatedUser).when(idamHelper).authenticate(anyString(), anyString());
        doReturn(userToken).when(authenticatedUser).getAccessToken();
        doReturn(serviceToken).when(authTokenGenerator).generate();
        headers = new HttpHeaders();
        headers.set("ServiceAuthorization", serviceToken);
        headers.set(HttpHeaders.AUTHORIZATION, userToken);
        doReturn(caseTypeReferences).when(defaultCaseDefinitionRepository).getCaseTypesReferences(headers);
        doReturn("email").when(applicationParams).getCacheWarmUpEmail();
        doReturn("password").when(applicationParams).getCacheWarmUpPassword();

        defaultCacheWarmUpService = new DefaultCacheWarmUpService(defaultCaseDefinitionRepository,
            authTokenGenerator,
            applicationParams,
            idamHelper);
    }

    @Test
    public void shouldWarmUpCache() {

        defaultCacheWarmUpService.warmUp();

        verify(defaultCaseDefinitionRepository).getCaseTypesReferences(headers);
        verify(defaultCaseDefinitionRepository).getCaseType("CT1", headers);
        verify(defaultCaseDefinitionRepository).getCaseType("CT2", headers);
        verify(defaultCaseDefinitionRepository).getCaseType("CT3", headers);
        verifyNoMoreInteractions(defaultCaseDefinitionRepository);
    }

    @Test
    public void shouldFailSafeWhenRetrievingCaseTypesEndinError() {

        doThrow(RestClientException.class).when(defaultCaseDefinitionRepository).getCaseType("CT2", headers);

        try {
            defaultCacheWarmUpService.warmUp();
        } catch(Exception e) {
            fail();
        }

        verify(defaultCaseDefinitionRepository).getCaseType("CT3", headers);
    }

}
